package com.wordbook.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordbook.domain.model.MatchKind
import com.wordbook.domain.model.SearchFilter
import com.wordbook.domain.model.SearchHit
import com.wordbook.domain.model.WordBadge
import com.wordbook.ui.components.StyleCard
import com.wordbook.ui.theme.LocalAppStyle
import com.wordbook.util.rememberSpeaker

/**
 * 单词搜索页。视觉对着 docs/style-samples/search-mockup.png 做：
 * 无边框胶囊搜索框、药丸筛选、每行一张风格化卡片（而不是 M3 默认的分隔线列表）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    totalWords: Int = 0,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val speaker = rememberSpeaker()
    val focusRequester = remember { FocusRequester() }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onBack)
                            .padding(10.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    SearchField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        onClear = viewModel::clearQuery,
                        focusRequester = focusRequester,
                        modifier = Modifier.weight(1f),
                    )
                }

                when {
                    state.query.isBlank() -> EmptyHints(
                        history = state.history,
                        totalWords = totalWords,
                        onUseHistory = viewModel::useHistory,
                        onClearHistory = viewModel::clearHistory,
                    )

                    state.searching -> Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }

                    else -> {
                        FilterRow(
                            counts = state.counts,
                            selected = state.filter,
                            onSelect = viewModel::setFilter,
                        )
                        ResultMeta(state)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(state.hits, key = { it.wordId }) { hit ->
                                StyleCard(onClick = { viewModel.openDetail(hit.wordId) }) {
                                    ResultRow(
                                        hit = hit,
                                        query = state.query,
                                        onSpeak = { speaker.speak(hit.headword) },
                                    )
                                }
                            }
                            if (state.hits.isEmpty()) {
                                item { NoResult(state.query) }
                            }
                            item { Spacer(Modifier.height(24.dp)) }
                        }
                    }
                }
            }

            state.detail?.let { detail ->
                ModalBottomSheet(
                    onDismissRequest = viewModel::dismissDetail,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                ) {
                    WordDetailContent(
                        detail = detail,
                        full = state.detailFull,
                        added = state.addedWordId == detail.wordId,
                        onToggleFull = viewModel::toggleDetailFull,
                        onAdd = { viewModel.addToToday(detail.wordId) },
                        onSpeak = { speaker.speak(detail.headword) },
                    )
                }
            }
        }
    }
}

/** 无边框胶囊搜索框（渲染图里那种），不用 M3 的描边输入框 */
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppStyle.current
    val shape = RoundedCornerShape(22.dp)
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = tokens.cardElevation.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Search,
                ),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = "搜索单词或中文释义",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .padding(vertical = 14.dp),
            )
            if (value.isNotEmpty()) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "清空",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onClear)
                        .padding(6.dp),
                )
            }
        }
    }
}

/** 药丸筛选标签（渲染图里那种），替代 M3 的 FilterChip */
@Composable
private fun StylePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppStyle.current
    val shape = RoundedCornerShape(if (tokens.cardCorner >= 16) 18.dp else 8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            )
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        shape,
                    )
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun EmptyHints(
    history: List<String>,
    totalWords: Int,
    onUseHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "最近搜索",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClearHistory) { Text("清空") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                history.take(4).forEach { item ->
                    StylePill(label = item, selected = false, onClick = { onUseHistory(item) })
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        StyleCard {
            HintLine("par", "前缀匹配 paragraph / parallel / party…")
            HintLine("妨碍", "直接输中文 → prevent / hamper / interfere")
            HintLine("running", "会自动还原成 run")
            Text(
                text = "· 输错了也没关系，会给相近的词",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        if (totalWords > 0) {
            Text(
                text = "词库共 " + totalWords + " 词 · 全部离线，不联网",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HintLine(keyword: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = keyword,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(70.dp),
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FilterRow(
    counts: Map<SearchFilter, Int>,
    selected: SearchFilter,
    onSelect: (SearchFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SearchFilter.entries.forEach { filter ->
            StylePill(
                label = filter.label + " " + (counts[filter] ?: 0),
                selected = selected == filter,
                onClick = { onSelect(filter) },
            )
        }
    }
}

@Composable
private fun ResultMeta(state: SearchUiState) {
    val total = state.counts[SearchFilter.ALL] ?: 0
    val text = when {
        state.lemma != null -> "词形还原：" + state.query + " → " + state.lemma
        state.matchKind == MatchKind.PREFIX -> total.toString() + " 个结果 · 前缀匹配"
        else -> total.toString() + " 个结果 · 按释义匹配"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 18.dp, top = 6.dp, bottom = 4.dp),
    )
}

@Composable
private fun ResultRow(hit: SearchHit, query: String, onSpeak: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = hit.headword,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                hit.phonetic?.let {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "/" + it + "/",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (hit.translation.isNotBlank()) {
                Text(
                    text = highlight(hit.translation.substringBefore("\n"), query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Badge(hit.badge)
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = "发音",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onSpeak)
                .padding(8.dp),
        )
    }
}

/** 把命中的中文加粗高亮（渲染图里那效果） */
internal fun highlight(text: String, query: String) = buildAnnotatedString {
    val q = query.trim()
    if (q.isEmpty() || !text.contains(q)) {
        append(text)
        return@buildAnnotatedString
    }
    var index = 0
    while (index < text.length) {
        val found = text.indexOf(q, index)
        if (found < 0) {
            append(text.substring(index))
            break
        }
        append(text.substring(index, found))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.Unspecified)) {
            append(text.substring(found, found + q.length))
        }
        index = found + q.length
    }
}

@Composable
private fun Badge(badge: WordBadge) {
    val (bg, fg) = when (badge.tone) {
        "hard" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        "due" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "learn" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(text = badge.label, style = MaterialTheme.typography.labelSmall, color = fg)
    }
}

@Composable
private fun NoResult(query: String) {
    StyleCard {
        Text("没找到「" + query + "」", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "试试中文释义（例如「妨碍」），或者少输几个字母",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
