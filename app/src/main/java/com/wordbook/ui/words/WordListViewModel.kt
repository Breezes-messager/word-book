package com.wordbook.ui.words

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordbook.data.db.WordEntity
import com.wordbook.data.repo.WordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WordListUiState(
    val words: List<WordEntity> = emptyList(),
    val loading: Boolean = false,
    val endReached: Boolean = false,
    val query: String = "",
    val total: Int = 0,
    val error: String? = null,
)

@HiltViewModel
class WordListViewModel @Inject constructor(
    private val wordRepository: WordRepository,
) : ViewModel() {

    private val pageSize = 60
    private val _state = MutableStateFlow(WordListUiState(loading = true))
    val state: StateFlow<WordListUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val total = wordRepository.totalCount().toInt()
            val first = wordRepository.page(pageSize, 0)
            _state.value = WordListUiState(words = first, total = total, endReached = first.size < pageSize)
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.endReached || current.query.isNotEmpty()) return
        _state.value = current.copy(loading = true)
        viewModelScope.launch {
            try {
                val more = wordRepository.page(pageSize, current.words.size)
                _state.value = current.copy(
                    words = current.words + more,
                    loading = false,
                    endReached = more.size < pageSize,
                )
            } catch (t: Throwable) {
                _state.value = current.copy(loading = false, error = "加载失败：" + (t.message ?: ""))
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query)
        if (query.isBlank()) {
            viewModelScope.launch {
                val first = wordRepository.page(pageSize, 0)
                _state.value = _state.value.copy(words = first, endReached = first.size < pageSize)
            }
            return
        }
        viewModelScope.launch {
            val result = wordRepository.searchPrefix(query.trim(), 60)
            _state.value = _state.value.copy(words = result, loading = false, endReached = true)
        }
    }
}
