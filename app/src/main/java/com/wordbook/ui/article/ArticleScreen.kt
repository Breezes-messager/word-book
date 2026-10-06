package com.wordbook.ui.article

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordbook.domain.article.ArticleContent
import com.wordbook.domain.article.ArticlePlan
import com.wordbook.domain.lemmatize.WordNormalizer
import com.wordbook.ui.components.ArticleParagraph
import com.wordbook.ui.dict.DictBottomSheet
import com.wordbook.ui.dict.DictSheetViewModel
import com.wordbook.ui.theme.articleBodyStyle
import com.wordbook.ui.theme.articleSecondaryStyle
import com.wordbook.ui.theme.readingTitleStyle
import com.wordbook.util.rememberSpeaker
import java.time.Instant
import kotlin.math.roundToInt
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 今日文章页：生成按钮 + 文章正文（目标词高亮、单词可点）+ 可折叠中文翻译。
 * 生成为空、断网或失败时展示上一次缓存，并明确提示。
 */
@Composable
fun ArticleScreen(
    onOpenSettings: () -> Unit,
    viewModel: ArticleViewModel = hiltViewModel(),
    dictViewModel: DictSheetViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dictState by dictViewModel.state.collectAsStateWithLifecycle()
    val speaker = rememberSpeaker()
    var translationExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("今日文章", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "用今天学过的词，让 DeepSeek 写一篇短文；点任意单词可查释义",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        GenerationCard(
            state = state,
            onGenerate = { viewModel.generate(regenerate = state.hasTodayArticle) },
            onOpenSettings = onOpenSettings,
        )

        // 断网 / 今天还没生成时，明确告诉用户看的是哪天的缓存
        state.cachedNotice?.let { notice ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = notice,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        state.notice?.let { notice ->
            Spacer(Modifier.height(8.dp))
            Text(notice, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }

        // 阅读调节：读文章时随手就能调，不用退回设置页
        Spacer(Modifier.height(10.dp))
        ReadingAdjuster(
            fontScale = state.fontScale,
            lineHeightScale = state.lineHeightScale,
            onFontScale = viewModel::setFontScale,
            onLineHeightScale = viewModel::setLineHeightScale,
        )
        state.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { viewModel.generate(regenerate = false) }) { Text("重试") }
        }

        Spacer(Modifier.height(12.dp))

        when {
            state.loading -> CircularProgressIndicator()
            state.content != null -> {
                if (state.todayArticles.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.todayArticles.forEachIndexed { index, _ ->
                            AssistChip(
                                onClick = { viewModel.selectArticle(index) },
                                label = { Text("第 " + (index + 1) + " 篇") },
                                leadingIcon = if (index == state.selectedIndex) {
                                    { Text("●") }
                                } else null,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                ArticleBody(
                    content = state.content!!,
                    translationExpanded = translationExpanded,
                    onToggleTranslation = { translationExpanded = !translationExpanded },
                    onWordClick = dictViewModel::lookup,
                    fontScale = state.fontScale,
                    lineHeightScale = state.lineHeightScale,
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    text = buildFooter(state.dateKey, state.createdAt, state.isSample, state.style),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> Text("还没有文章", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(Modifier.height(32.dp))
    }

    dictState?.let { sheetState ->
        DictBottomSheet(
            state = sheetState,
            onDismiss = dictViewModel::dismiss,
            onSpeak = speaker::speak,
            onSelectWord = dictViewModel::lookup,
        )
    }
}

@Composable
private fun GenerationCard(
    state: ArticleUiState,
    onGenerate: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "今天学过 " + state.todayWordCount + " 个词，可以生成 " + state.batchCount + " 篇文章",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "每篇使用 " + ArticlePlan.MIN_WORDS + "–" + ArticlePlan.MAX_PER_ARTICLE + " 个目标词，已掌握的词会作为背景词汇一起发送",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            when {
                state.generating -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = state.progressText ?: "正在生成…",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                !state.hasApiKey -> {
                    Text(
                        text = "还没有配置 DeepSeek API Key",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onOpenSettings) { Text("去设置里填写") }
                }
                else -> {
                    Button(
                        onClick = onGenerate,
                        enabled = state.canGenerate,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (state.hasTodayArticle) "重新生成今日文章" else "生成今日文章")
                    }
                    if (!state.canGenerate) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "今天学的词太少，攒够 " + ArticlePlan.MIN_WORDS + " 个再来",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

/** 字号 / 行距的快捷调节条 */
@Composable
private fun ReadingAdjuster(
    fontScale: Float,
    lineHeightScale: Float,
    onFontScale: (Float) -> Unit,
    onLineHeightScale: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "阅读",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SmallStepButton("A−") { onFontScale(fontScale - 0.05f) }
        Text(
            text = (fontScale * 100).roundToInt().toString() + "%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SmallStepButton("A+") { onFontScale(fontScale + 0.05f) }
        Spacer(Modifier.width(8.dp))
        SmallStepButton("行距−") { onLineHeightScale(lineHeightScale - 0.05f) }
        SmallStepButton("行距+") { onLineHeightScale(lineHeightScale + 0.05f) }
    }
}

@Composable
private fun SmallStepButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(34.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ArticleBody(
    content: ArticleContent,
    translationExpanded: Boolean,
    onToggleTranslation: () -> Unit,
    onWordClick: (String) -> Unit,
    fontScale: Float,
    lineHeightScale: Float,
) {
    val highlights = remember(content) {
        buildSet {
            content.occurrences.forEach { occurrence ->
                add(WordNormalizer.normalize(occurrence.surface))
                add(WordNormalizer.normalize(occurrence.target))
            }
        }
    }

    Text(
        text = content.title,
        style = readingTitleStyle(MaterialTheme.typography.titleLarge, fontScale),
        fontWeight = FontWeight.Bold,
    )
    if (content.titleCn.isNotBlank()) {
        Text(
            text = content.titleCn,
            style = articleSecondaryStyle(fontScale, lineHeightScale),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(12.dp))

    content.paragraphs.forEachIndexed { index, paragraph ->
        ArticleParagraph(
            text = paragraph,
            highlights = highlights,
            onWordClick = onWordClick,
            modifier = Modifier.fillMaxWidth(),
            style = articleBodyStyle(fontScale, lineHeightScale),
        )
        Spacer(Modifier.height(12.dp))
        if (translationExpanded && index < content.translation.size) {
            Text(
                text = content.translation[index],
                style = articleSecondaryStyle(fontScale, lineHeightScale),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
        }
    }

    if (content.translation.isNotEmpty()) {
        OutlinedButton(onClick = onToggleTranslation) {
            Text(if (translationExpanded) "收起中文翻译" else "显示中文翻译")
        }
    }
}

private val footerFormatter = DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日 HH:mm")

private fun buildFooter(dateKey: String, createdAt: Long?, isSample: Boolean, style: String): String {
    if (isSample) return "这是内置示例文章（含 running / went 等变形词），配置 API Key 后可以生成自己的文章"
    val time = createdAt?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(footerFormatter)
    } ?: dateKey
    return "风格：" + style + " · 生成时间：" + time
}
