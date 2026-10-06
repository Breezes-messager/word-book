package com.wordbook.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordbook.data.db.ArticleEntity
import com.wordbook.data.db.WordEntity
import com.wordbook.data.repo.StatsRepository
import com.wordbook.data.repo.StudyRepository
import com.wordbook.data.repo.TodayTask
import com.wordbook.data.repo.WordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val importProgress: Int? = null,
    val task: TodayTask? = null,
    val totalWords: Int = 0,
    val streak: Int = 0,
    val previewWords: List<WordEntity> = emptyList(),
    val latestArticle: ArticleEntity? = null,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val studyRepository: StudyRepository,
    private val statsRepository: StatsRepository,
) : ViewModel() {

    private val loading = MutableStateFlow(true)
    private val importProgress = MutableStateFlow<Int?>(null)
    private val refreshTrigger = MutableStateFlow(0)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        loading, importProgress, refreshTrigger, error,
    ) { isLoading, progress, _, err -> Triple(isLoading, progress, err) }
        .map { (isLoading, progress, err) -> load(isLoading, progress, err) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        bootstrap()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            try {
                wordRepository.ensureDataReady { done -> importProgress.value = done }
            } catch (t: Throwable) {
                error.value = "词库准备失败：" + (t.message ?: t.javaClass.simpleName)
            } finally {
                loading.value = false
                refresh()
            }
        }
    }

    fun refresh() {
        refreshTrigger.value++
    }

    private suspend fun load(isLoading: Boolean, progress: Int?, err: String?): HomeUiState {
        if (isLoading) return HomeUiState(loading = true, importProgress = progress)
        return try {
            val task = studyRepository.todayTask()
            HomeUiState(
                loading = false,
                task = task,
                totalWords = wordRepository.totalCount().toInt(),
                streak = statsRepository.streak(),
                previewWords = wordRepository.firstWords(20),
                // 注意：Room 的 Flow 会一直观察数据库，这里只取当前值，必须用 first()
                latestArticle = statsRepository.recentArticles(1).first().firstOrNull(),
                error = err,
            )
        } catch (t: Throwable) {
            HomeUiState(loading = false, error = "读取数据失败：" + (t.message ?: t.javaClass.simpleName))
        }
    }
}
