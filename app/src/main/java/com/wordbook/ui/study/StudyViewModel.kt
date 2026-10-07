package com.wordbook.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordbook.data.repo.StudyRepository
import com.wordbook.data.repo.StudyWord
import com.wordbook.domain.fsrs.Rating
import com.wordbook.util.formatInterval
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SessionMode { NEW, REVIEW }

data class StudyUiState(
    val loading: Boolean = true,
    val mode: SessionMode = SessionMode.NEW,
    val current: StudyWord? = null,
    val index: Int = 0,
    val total: Int = 0,
    val flipped: Boolean = false,
    /** 四档评分各自会把卡片排到多久之后（毫秒） */
    val previews: Map<Rating, Long> = emptyMap(),
    val finished: Boolean = false,
    val empty: Boolean = false,
    val error: String? = null,
    /** 刚评完分的反馈文字（"良好 · 10 分钟"），短暂显示一下 */
    val feedback: String? = null,
    val feedbackToken: Long = 0L,
)

@HiltViewModel
class StudyViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StudyUiState())
    val state: StateFlow<StudyUiState> = _state.asStateFlow()

    private var queue: List<StudyWord> = emptyList()

    /** 在 queue 里的当前位置（"重来"补队会让 queue 变长，所以位置和显示进度要分开） */
    private var pos: Int = 0

    /** 本轮真正完成的不重复卡片；对外显示的进度只认这个 */
    private val completedIds = mutableSetOf<Long>()

    /** 本轮涉及的不重复卡片总数。对用户恒定不变 —— 点"重来"补队也不会让分母变大 */
    private var distinctTotal: Int = 0

    private var cardShownAt: Long = System.currentTimeMillis()

    /** 本轮会话里已经因为“重来”而重新入队的卡，避免无限循环 */
    private val requeued = mutableSetOf<Long>()

    fun start(mode: SessionMode) {
        if (_state.value.total > 0 && _state.value.mode == mode && !_state.value.finished) return
        _state.value = StudyUiState(loading = true, mode = mode)
        queue = emptyList()
        pos = 0
        distinctTotal = 0
        completedIds.clear()
        requeued.clear()
        viewModelScope.launch {
            try {
                queue = when (mode) {
                    SessionMode.NEW -> studyRepository.newWordSession()
                    SessionMode.REVIEW -> studyRepository.reviewSession()
                }
                if (queue.isEmpty()) {
                    _state.value = StudyUiState(loading = false, mode = mode, empty = true)
                    return@launch
                }
                distinctTotal = queue.map { it.card.id }.distinct().size
                _state.value = StudyUiState(
                    loading = false,
                    mode = mode,
                    current = queue.first(),
                    index = 0,
                    total = distinctTotal,
                )
                cardShownAt = System.currentTimeMillis()
                loadPreviews()
            } catch (t: Throwable) {
                _state.value = StudyUiState(
                    loading = false,
                    mode = mode,
                    error = "准备会话失败：" + (t.message ?: t.javaClass.simpleName),
                )
            }
        }
    }

    fun flip() {
        _state.update { it.copy(flipped = true) }
    }

    /** 四档评分：写入 FSRS 排程 + 复习日志，然后进入下一张 */
    fun rate(rating: Rating) {
        val current = _state.value.current ?: return
        viewModelScope.launch {
            try {
                val duration = System.currentTimeMillis() - cardShownAt
                val result = studyRepository.rate(current, rating, duration)
                _state.update {
                    it.copy(
                        feedback = ratingLabel(rating) + " · " + formatInterval(result.intervalAfterMs),
                        feedbackToken = System.currentTimeMillis(),
                    )
                }

                // “重来”的卡在本轮里再出现一次（每张卡最多一次），符合学习步的逻辑。
                // 注意：补队只影响内部队列，不会让右上角的总数变大。
                val cardId = current.card.id
                if (rating == Rating.AGAIN && requeued.add(cardId)) {
                    val refreshed = studyRepository.cardById(cardId) ?: current.card
                    queue = queue + current.copy(card = refreshed)
                } else {
                    // 不是“重来”，或者这张卡本轮已经补过一次队了 → 这张卡算完成
                    completedIds.add(cardId)
                }
                advance()
            } catch (t: Throwable) {
                _state.update { it.copy(error = "保存失败：" + (t.message ?: t.javaClass.simpleName)) }
            }
        }
    }

    /** 反馈显示完就清掉 */
    fun clearFeedback() {
        _state.update { it.copy(feedback = null) }
    }

    private fun ratingLabel(rating: Rating): String = when (rating) {
        Rating.AGAIN -> "重来"
        Rating.HARD -> "困难"
        Rating.GOOD -> "良好"
        Rating.EASY -> "简单"
    }

    private suspend fun loadPreviews() {
        val current = _state.value.current ?: return
        val previews = runCatching {
            studyRepository.previewIntervals(current.card).mapValues { (_, duration) -> duration.toMillis() }
        }.getOrDefault(emptyMap())
        _state.update { it.copy(previews = previews) }
    }

    private suspend fun advance() {
        val next = pos + 1
        // 显示的进度 = 已完成的不重复卡片数（+1 表示"正在做第几张"），分母恒定
        val done = completedIds.size
        if (next >= queue.size) {
            _state.update {
                it.copy(finished = true, current = null, index = done, total = distinctTotal)
            }
            return
        }
        pos = next
        val nextCard = queue[next]
        _state.update {
            it.copy(
                current = nextCard,
                index = done,
                total = distinctTotal,
                flipped = false,
                previews = emptyMap(),
            )
        }
        cardShownAt = System.currentTimeMillis()
        loadPreviews()
    }
}
