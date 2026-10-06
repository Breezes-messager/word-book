package com.wordbook.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordbook.data.db.ArticleEntity
import com.wordbook.data.repo.StatsRepository
import com.wordbook.data.repo.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DayCount(val dayKey: String, val count: Int)

data class StatsUiState(
    val loading: Boolean = true,
    val learnedTotal: Int = 0,
    val streak: Int = 0,
    val totalReviews: Int = 0,
    val wordBankSize: Int = 0,
    val todayNew: Int = 0,
    val todayReview: Int = 0,
    val distribution: Map<String, Int> = emptyMap(),
    val days: List<DayCount> = emptyList(),
    val articles: List<ArticleEntity> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    private val studyRepository: StudyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                val task = studyRepository.todayTask()
                _state.value = StatsUiState(
                    loading = false,
                    learnedTotal = statsRepository.learnedTotal(),
                    streak = statsRepository.streak(),
                    totalReviews = statsRepository.totalReviews(),
                    wordBankSize = statsRepository.wordBankSize(),
                    todayNew = task.newDone,
                    todayReview = task.reviewDone,
                    distribution = statsRepository.stateDistribution(),
                    days = statsRepository.dailyCounts(30).map { DayCount(it.first, it.second) },
                    // Room 的 Flow 不会结束，取当前值要用 first()
                    articles = statsRepository.recentArticles(10).first(),
                )
            } catch (t: Throwable) {
                _state.value = StatsUiState(
                    loading = false,
                    error = "统计数据加载失败：" + (t.message ?: t.javaClass.simpleName),
                )
            }
        }
    }
}
