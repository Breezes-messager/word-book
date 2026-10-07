package com.wordbook.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordbook.data.prefs.SettingsRepository
import com.wordbook.data.repo.SearchRepository
import com.wordbook.data.repo.WordRepository
import com.wordbook.data.repo.examples
import com.wordbook.data.repo.phrases
import com.wordbook.data.repo.relatedWords
import com.wordbook.data.repo.synonyms
import com.wordbook.data.repo.translationLines
import com.wordbook.domain.model.MatchKind
import com.wordbook.domain.model.SearchFilter
import com.wordbook.domain.model.SearchHit
import com.wordbook.domain.model.WordBadge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 底部词义卡的内容（点搜索结果时加载） */
data class WordDetail(
    val wordId: Long,
    val headword: String,
    val phonetic: String?,
    val translationLines: List<String> = emptyList(),
    val definitionLines: List<String> = emptyList(),
    val remMethod: String? = null,
    val examples: List<Pair<String, String>> = emptyList(),
    val synonyms: List<Pair<String, List<String>>> = emptyList(),
    val relatedWords: List<Pair<String, String>> = emptyList(),
    val phrases: List<Pair<String, String>> = emptyList(),
    val badge: WordBadge? = null,
)

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val hits: List<SearchHit> = emptyList(),
    val counts: Map<SearchFilter, Int> = emptyMap(),
    val matchKind: MatchKind = MatchKind.PREFIX,
    val lemma: String? = null,
    val history: List<String> = emptyList(),
    val searching: Boolean = false,
    val detail: WordDetail? = null,
    /** 底部卡片是否展开成完整词卡 */
    val detailFull: Boolean = false,
    val addedWordId: Long? = null,
    val message: String? = null,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val settingsRepository: SettingsRepository,
    private val wordRepository: WordRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            _state.update { it.copy(history = settingsRepository.current().historyList()) }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            // 输入防抖：打字时不至于每敲一个字母查一次库
            delay(180)
            runSearch()
        }
    }

    fun setFilter(filter: SearchFilter) {
        _state.update { it.copy(filter = filter) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { runSearch() }
    }

    fun clearQuery() {
        searchJob?.cancel()
        _state.update { it.copy(query = "", hits = emptyList(), counts = emptyMap(), lemma = null) }
    }

    fun useHistory(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { runSearch() }
    }

    fun clearHistory() {
        viewModelScope.launch {
            settingsRepository.clearSearchHistory()
            _state.update { it.copy(history = emptyList()) }
        }
    }

    private suspend fun runSearch() {
        val current = _state.value
        if (current.query.isBlank()) {
            _state.update { it.copy(hits = emptyList(), counts = emptyMap(), searching = false) }
            return
        }
        _state.update { it.copy(searching = true) }
        val outcome = runCatching { searchRepository.search(current.query, current.filter) }
            .getOrElse { error ->
                _state.update { it.copy(searching = false, message = "搜索失败：" + (error.message ?: "")) }
                return
            }
        _state.update {
            it.copy(
                hits = outcome.hits,
                counts = outcome.counts,
                matchKind = outcome.matchKind,
                lemma = outcome.lemma,
                searching = false,
            )
        }
    }

    /** 点开一行：加载完整词义 + 记一条搜索历史 */
    fun openDetail(wordId: Long) {
        viewModelScope.launch {
            // 打开了词义卡说明这次搜索有用，记进历史
            settingsRepository.pushSearchHistory(_state.value.query)
            val word = wordRepository.byId(wordId) ?: return@launch
            val badge = _state.value.hits.firstOrNull { it.wordId == wordId }?.badge
            _state.update {
                it.copy(
                    detailFull = false,
                    detail = WordDetail(
                        wordId = wordId,
                        headword = word.headword,
                        phonetic = (word.phoneticUs ?: word.phoneticUk)?.takeIf { p -> p.isNotBlank() },
                        translationLines = word.translationLines(),
                        definitionLines = word.transEn.orEmpty().split("\n").filter { l -> l.isNotBlank() },
                        remMethod = word.remMethod?.takeIf { m -> m.isNotBlank() },
                        examples = word.examples().map { e -> e.en to e.cn },
                        synonyms = word.synonyms().map { g -> g.pos + " " + g.tran to g.words },
                        relatedWords = word.relatedWords().flatMap { g ->
                            g.words.map { (g.pos + " " + it.hwd).trim() to it.tran }
                        },
                        phrases = word.phrases().map { p -> p.en to p.cn },
                        badge = badge,
                    ),
                )
            }
        }
    }

    fun toggleDetailFull() {
        _state.update { it.copy(detailFull = !it.detailFull) }
    }

    fun dismissDetail() {
        _state.update { it.copy(detail = null, detailFull = false) }
    }

    /** 加入今日学习 + 记搜索历史 */
    fun addToToday(wordId: Long) {
        viewModelScope.launch {
            val result = searchRepository.addToToday(wordId)
            settingsRepository.pushSearchHistory(_state.value.query)
            _state.update {
                it.copy(
                    addedWordId = wordId,
                    history = settingsRepository.current().historyList(),
                    message = when (result) {
                        SearchRepository.AddResult.Added -> "已加入今日学习"
                        SearchRepository.AddResult.MovedToReview -> "已到期，下次「开始复习」会出现"
                    },
                )
            }
            // 状态变了，列表要跟着刷新
            runSearch()
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }
}
