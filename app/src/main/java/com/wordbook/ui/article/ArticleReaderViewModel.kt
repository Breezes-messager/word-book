package com.wordbook.ui.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordbook.data.prefs.SettingsRepository
import com.wordbook.data.repo.ArticleRepository
import com.wordbook.domain.article.ArticleContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArticleReaderUiState(
    val loading: Boolean = true,
    val content: ArticleContent? = null,
    val title: String = "",
    val titleCn: String = "",
    val dateKey: String = "",
    val style: String = "",
    val createdAt: Long? = null,
    val fontScale: Float = 1.0f,
    val lineHeightScale: Float = 1.0f,
    val isSample: Boolean = false,
    val error: String? = null,
)

/** 读一篇已经存下来的文章（历史文章列表点进来的） */
@HiltViewModel
class ArticleReaderViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ArticleReaderUiState())
    val state: StateFlow<ArticleReaderUiState> = _state.asStateFlow()

    /** 已经加载过的文章 id，避免旋转屏幕 / 重新组合时反复查库 */
    private var loadedId: Long? = null

    fun load(id: Long) {
        if (loadedId == id) return
        loadedId = id
        viewModelScope.launch {
            try {
                val entity = articleRepository.articleById(id)
                if (entity == null) {
                    _state.value = ArticleReaderUiState(loading = false, error = "这篇文章已经不在了")
                    return@launch
                }
                val settings = settingsRepository.current()
                val content = articleRepository.contentOf(entity)
                // 打开就算读过了；只记第一次
                articleRepository.markRead(id)
                _state.value = ArticleReaderUiState(
                    loading = false,
                    content = content,
                    title = entity.title,
                    titleCn = entity.titleCn,
                    dateKey = entity.dateKey,
                    style = entity.style,
                    createdAt = entity.createdAt,
                    fontScale = settings.articleFontScale,
                    lineHeightScale = settings.articleLineHeightScale,
                    isSample = false,
                    error = if (content == null) "这篇文章的内容解析失败" else null,
                )
            } catch (t: Throwable) {
                _state.value = ArticleReaderUiState(
                    loading = false,
                    error = "读取失败：" + (t.message ?: t.javaClass.simpleName),
                )
            }
        }
    }
}
