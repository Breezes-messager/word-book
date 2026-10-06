package com.wordbook.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordbook.data.repo.StudyRepository
import com.wordbook.data.repo.StudyWord
import com.wordbook.domain.fsrs.Rating
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
)

@HiltViewModel
class StudyViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StudyUiState())
    val state: StateFlow<StudyUiState> = _state.asStateFlow()

    private var queue: List<StudyWord> = emptyList()
    private var cardShownAt: Long = System.currentTimeMillis()

    /** 本轮会话里已经因为“重来”而重新入队的卡，避免无限循环 */
    private val requeued = mutableSetOf<Long>()

    fun start(mode: SessionMode) {
        if (_state.value.total > 0 && _state.value.mode == mode && !_state.value.finished) return
        _state.value = StudyUiState(loading = true, mode = mode)
        queue = emptyList()
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
                _state.value = StudyUiState(
                    loading = false,
                    mode = mode,
                    current = queue.first(),
                    index = 0,
                    total = queue.size,
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
                studyRepository.rate(current, rating, duration)

                // “重来”的卡在本轮里再出现一次（每张卡最多一次），符合学习步的逻辑
                if (rating == Rating.AGAIN && requeued.add(current.card.id)) {
                    val refreshed = studyRepository.cardById(current.card.id) ?: current.card
                    queue = queue + current.copy(card = refreshed)
                }
                advance()
            } catch (t: Throwable) {
                _state.update { it.copy(error = "保存失败：" + (t.message ?: t.javaClass.simpleName)) }
            }
        }
    }

    private suspend fun loadPreviews() {
        val current = _state.value.current ?: return
        val previews = runCatching {
            studyRepository.previewIntervals(current.card).mapValues { (_, duration) -> duration.toMillis() }
        }.getOrDefault(emptyMap())
        _state.update { it.copy(previews = previews) }
    }

    private suspend fun advance() {
        val next = _state.value.index + 1
        if (next >= queue.size) {
            _state.update { it.copy(finished = true, current = null, index = next) }
            return
        }
        val nextCard = queue[next]
        _state.update {
            it.copy(
                current = nextCard,
                index = next,
                total = queue.size,
                flipped = false,
                previews = emptyMap(),
            )
        }
        cardShownAt = System.currentTimeMillis()
        loadPreviews()
    }
}
