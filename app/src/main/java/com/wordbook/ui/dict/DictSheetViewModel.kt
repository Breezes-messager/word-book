package com.wordbook.ui.dict

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordbook.data.dict.DictEntry
import com.wordbook.data.dict.DictLookupResult
import com.wordbook.data.dict.DictRepository
import com.wordbook.data.repo.SentencePair
import com.wordbook.data.repo.WordRepository
import com.wordbook.data.repo.examples
import com.wordbook.data.repo.phrases
import com.wordbook.domain.lemmatize.WordNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DictSheetUiState(
    val query: String = "",
    val loading: Boolean = true,
    val headline: String = "",
    val phonetic: String? = null,
    /** 中文释义 */
    val translation: String? = null,
    /** 英英释义 */
    val definition: String? = null,
    val exchange: String? = null,
    /** 词形还原提示，例如 running -> run */
    val lemmaNote: String? = null,
    val examples: List<SentencePair> = emptyList(),
    val phrases: List<SentencePair> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val notFound: Boolean = false,
    val error: String? = null,
)

/** 点击查词弹层的 ViewModel：完全离线，先查 ECDICT，再补词书里的例句 */
@HiltViewModel
class DictSheetViewModel @Inject constructor(
    private val dictRepository: DictRepository,
    private val wordRepository: WordRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<DictSheetUiState?>(null)
    val state: StateFlow<DictSheetUiState?> = _state.asStateFlow()

    fun dismiss() {
        _state.value = null
    }

    fun lookup(raw: String) {
        _state.value = DictSheetUiState(query = raw, loading = true)
        viewModelScope.launch {
            try {
                _state.value = build(raw, dictRepository.lookup(raw))
            } catch (t: Throwable) {
                _state.value = DictSheetUiState(
                    query = raw,
                    loading = false,
                    error = "查词失败：" + (t.message ?: t.javaClass.simpleName),
                )
            }
        }
    }

    private suspend fun build(raw: String, result: DictLookupResult): DictSheetUiState = when (result) {
        is DictLookupResult.Exact -> enrich(raw, result.entry, null, result.entry)
        is DictLookupResult.Lemmatized -> enrich(raw, result.entry, result.lemma, result.entry)
        is DictLookupResult.Suggestions -> DictSheetUiState(
            query = raw,
            loading = false,
            headline = raw,
            suggestions = result.entries.map { it.word },
        )
        is DictLookupResult.NotFound -> DictSheetUiState(
            query = raw,
            loading = false,
            headline = raw,
            notFound = true,
        )
    }

    private suspend fun enrich(
        raw: String,
        entry: DictEntry,
        lemma: String?,
        rawEntry: DictEntry,
    ): DictSheetUiState {
        // 词书里可能有更完整的例句 / 短语
        val book = wordRepository.byHeadword(entry.word)
        val translation = entry.translation?.takeIf { it.isNotBlank() } ?: book?.transCn
        val definition = entry.definition?.takeIf { it.isNotBlank() } ?: book?.transEn
        val phonetic = entry.phonetic?.takeIf { it.isNotBlank() } ?: book?.phoneticUs ?: book?.phoneticUk
        // 提示形如 "actioned -> action"：要和用户点的那个形态比，而不是和还原后的词条比
        val surface = WordNormalizer.normalize(raw)
        return DictSheetUiState(
            query = raw,
            loading = false,
            headline = entry.word,
            phonetic = phonetic,
            translation = translation,
            definition = definition,
            exchange = rawEntry.exchange,
            lemmaNote = if (lemma != null && !lemma.equals(surface, ignoreCase = true)) {
                surface + " -> " + lemma
            } else null,
            examples = book?.examples() ?: emptyList(),
            phrases = book?.phrases() ?: emptyList(),
        )
    }
}
