package com.wordbook.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordbook.ui.components.StyleCard
import com.wordbook.ui.components.StyleProgress
import com.wordbook.ui.components.StyleSectionTitle
import com.wordbook.ui.components.WordRow
import com.wordbook.util.rememberSpeaker

@Composable
fun HomeScreen(
    onStartStudy: () -> Unit,
    onStartReview: () -> Unit,
    onOpenArticle: () -> Unit,
    onOpenWordList: () -> Unit,
    onOpenStats: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val speaker = rememberSpeaker()

    LaunchedEffect(Unit) { viewModel.refresh() }

    if (state.loading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            val progress = state.importProgress
            Text(
                text = if (progress == null) "正在准备词库…" else "正在导入词库… " + progress + " 个词",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                TodayTaskCard(
                    newDone = state.task?.newDone ?: 0,
                    newTarget = state.task?.newTarget ?: 0,
                    reviewDone = state.task?.reviewDone ?: 0,
                    reviewDue = state.task?.reviewDue ?: 0,
                    streak = state.streak,
                    totalWords = state.totalWords,
                    onStartStudy = onStartStudy,
                    onStartReview = onStartReview,
                    onOpenArticle = onOpenArticle,
                )
                state.error?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(8.dp))
                state.latestArticle?.let { article ->
                    StyleCard(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        onClick = onOpenArticle,
                    ) {
                        Column {
                            Text("最近生成的文章", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = article.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = article.dateKey + " · " + article.titleCn,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StyleSectionTitle("词书预览（前 20 个词）")
                    TextButton(onClick = onOpenWordList) { Text("查看全部") }
                }
            }
            HorizontalDivider()
        }
        items(state.previewWords, key = { it.id }) { word ->
            WordRow(word = word, onSpeak = speaker::speak)
        }
        item {
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TodayTaskCard(
    newDone: Int,
    newTarget: Int,
    reviewDone: Int,
    reviewDue: Int,
    streak: Int,
    totalWords: Int,
    onStartStudy: () -> Unit,
    onStartReview: () -> Unit,
    onOpenArticle: () -> Unit,
) {
    StyleCard {
        Column {
            StyleSectionTitle("今日任务")
            Spacer(Modifier.height(12.dp))

            Text(
                text = "新词 " + newDone + " / " + if (newTarget <= 0) "不限" else newTarget.toString(),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(4.dp))
            StyleProgress(progress = if (newTarget <= 0) 0f else (newDone.toFloat() / newTarget).coerceIn(0f, 1f))
            Spacer(Modifier.height(12.dp))

            Text("待复习 " + reviewDue + " 个（今日已复习 " + reviewDone + "）", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            StyleProgress(
                progress = run {
                    val total = (reviewDue + reviewDone).toFloat()
                    if (total <= 0f) 0f else (reviewDone.toFloat() / total).coerceIn(0f, 1f)
                },
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = "连续打卡 " + streak + " 天 · 词库共 " + totalWords + " 词",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onStartStudy, modifier = Modifier.weight(1f)) { Text("开始学习") }
                OutlinedButton(onClick = onStartReview, modifier = Modifier.weight(1f)) { Text("开始复习") }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenArticle, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text("  今日文章")
            }
        }
    }
}
