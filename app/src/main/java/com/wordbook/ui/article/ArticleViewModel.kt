package com.wordbook.ui.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordbook.data.db.ArticleEntity
import com.wordbook.data.prefs.SettingsRepository
import com.wordbook.data.repo.ArticleRepository
import com.wordbook.domain.article.ArticleContent
import com.wordbook.domain.article.ArticlePlan
import com.wordbook.domain.article.SampleArticle
import com.wordbook.util.prettyDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArticleUiState(
    val loading: Boolean = true,
    /** 正在展示的文章内容 */
    val content: ArticleContent? = null,
    /** 今天已经生成的文章（可能多篇） */
    val todayArticles: List<ArticleEntity> = emptyList(),
    val selectedIndex: Int = 0,
    val createdAt: Long? = null,
    val dateKey: String = "",
    val style: String = "",
    /** 展示的是内置示例文章 */
    val isSample: Boolean = false,
    /** 断网 / 生成失败时展示的是历史缓存 */
    val cachedNotice: String? = null,
    val todayWordCount: Int = 0,
    val batchCount: Int = 0,
    val hasApiKey: Boolean = false,
    val generating: Boolean = false,
    val progressText: String? = null,
    val notice: String? = null,
    val error: String? = null,
    val fontScale: Float = 1.0f,
    val lineHeightScale: Float = 1.0f,
) {
    val canGenerate: Boolean get() = todayWordCount >= ArticlePlan.MIN_WORDS
    val hasTodayArticle: Boolean get() = todayArticles.isNotEmpty()
}

@HiltViewModel
class ArticleViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ArticleUiState())
    val state: StateFlow<ArticleUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                val settings = settingsRepository.current()
                val todayWords = articleRepository.todayStudiedWords()
                val batches = articleRepository.planBatches(todayWords)
                val today = articleRepository.todayArticles()

                val base = ArticleUiState(
                    loading = false,
                    todayWordCount = todayWords.size,
                    batchCount = batches.size,
                    hasApiKey = settings.apiKey.isNotBlank(),
                    todayArticles = today,
                    style = settings.articleStyle.label,
                    fontScale = settings.articleFontScale,
                    lineHeightScale = settings.articleLineHeightScale,
                )

                if (today.isNotEmpty()) {
                    // 恢复上次看的那一篇：切到别的 Tab 再回来时 ViewModel 会重建，
                    // 不能总是回到第一篇
                    val index = restoreIndex(settings.lastArticleSelection, today)
                    _state.value = withArticle(base, today[index], selectedIndex = index)
                    return@launch
                }

                // 今天还没生成：优先展示上一次缓存，其次展示内置示例
                val latest = articleRepository.latestArticle()
                if (latest != null) {
                    val content = articleRepository.contentOf(latest)
                    if (content != null) {
                        _state.value = base.copy(
                            content = content,
                            dateKey = latest.dateKey,
                            createdAt = latest.createdAt,
                            style = latest.style,
                            cachedNotice = "当前为 " + prettyDate(latest.dateKey) + "缓存的内容（今天还没有生成新的文章）",
                        )
                        return@launch
                    }
                }

                _state.value = base.copy(
                    content = SampleArticle.content,
                    isSample = true,
                    dateKey = "",
                    style = "示例",
                )
            } catch (t: Throwable) {
                _state.value = ArticleUiState(
                    loading = false,
                    error = "加载文章失败：" + (t.message ?: t.javaClass.simpleName),
                )
            }
        }
    }

    private fun withArticle(base: ArticleUiState, article: ArticleEntity, selectedIndex: Int): ArticleUiState {
        val content = articleRepository.contentOf(article)
        if (content == null) {
            return base.copy(error = "缓存的文章内容已损坏，可以重新生成")
        }
        return base.copy(
            content = content,
            selectedIndex = selectedIndex,
            createdAt = article.createdAt,
            dateKey = article.dateKey,
            style = article.style,
            isSample = false,
            cachedNotice = null,
        )
    }

    fun selectArticle(index: Int) {
        val article = _state.value.todayArticles.getOrNull(index) ?: return
        _state.value = withArticle(_state.value, article, index)
        viewModelScope.launch {
            settingsRepository.setLastArticleSelection(selectionKey(article))
        }
    }

    /** "日期:批次"，同一天里定位到具体那一篇 */
    private fun selectionKey(article: ArticleEntity): String =
        article.dateKey + ":" + article.batchIndex

    /** 从上次的记录里找出今天的第几篇；找不到就回到第一篇 */
    private fun restoreIndex(saved: String, today: List<ArticleEntity>): Int {
        if (saved.isBlank()) return 0
        val index = today.indexOfFirst { selectionKey(it) == saved }
        return if (index >= 0) index else 0
    }

    fun generate(regenerate: Boolean = false) {
        if (_state.value.generating) return
        viewModelScope.launch {
            _state.update {
                it.copy(generating = true, error = null, notice = null, progressText = "正在准备…")
            }
            try {
                val report = articleRepository.generateToday(regenerate) { done, total ->
                    _state.update { it.copy(progressText = "正在生成第 " + done + " / " + total + " 篇…") }
                }
                val today = articleRepository.todayArticles()
                if (report.savedCount == 0) {
                    _state.update {
                        it.copy(
                            generating = false,
                            progressText = null,
                            error = report.error ?: "生成失败，请稍后重试",
                            todayArticles = today,
                        )
                    }
                    return@launch
                }
                _state.update {
                    it.copy(
                        generating = false,
                        progressText = null,
                        todayArticles = today,
                        notice = if (report.failedCount > 0) {
                            "部分文章生成失败（" + report.failedCount + " 篇）：" + (report.error ?: "")
                        } else {
                            "已生成 " + report.savedCount + " 篇文章，已保存到本地"
                        },
                    )
                }
                withArticle(_state.value, today.first(), 0).let { _state.value = it }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        generating = false,
                        progressText = null,
                        error = "生成失败：" + (t.message ?: t.javaClass.simpleName),
                    )
                }
            }
        }
    }

    /** 把内置示例文章存进缓存，方便之后离线查看 */
    fun saveSampleToCache() {
        viewModelScope.launch {
            runCatching { articleRepository.saveManual(SampleArticle.content, "示例") }
            load()
        }
    }

    /** 调正文字号：先改界面（立即生效），再落盘 */
    fun setFontScale(scale: Float) {
        val clamped = scale.coerceIn(0.85f, 1.6f)
        _state.update { it.copy(fontScale = clamped) }
        viewModelScope.launch { settingsRepository.setArticleFontScale(clamped) }
    }

    /** 调行距 */
    fun setLineHeightScale(scale: Float) {
        val clamped = scale.coerceIn(1.0f, 1.9f)
        _state.update { it.copy(lineHeightScale = clamped) }
        viewModelScope.launch { settingsRepository.setArticleLineHeightScale(clamped) }
    }

    fun dismissMessage() {
        _state.update { it.copy(error = null, notice = null) }
    }
}
