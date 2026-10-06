package com.wordbook.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordbook.domain.fsrs.Rating
import com.wordbook.ui.components.StudyCardView
import com.wordbook.util.formatInterval
import com.wordbook.util.rememberSpeaker

/**
 * 学习 / 复习共用的界面。
 * NEW 模式：正面单词音标 → 翻面看释义 → 四档评分
 * REVIEW 模式：先只看单词，回忆后点“显示答案”，再四档评分
 */
@Composable
fun StudyScreen(
    mode: SessionMode,
    onBack: () -> Unit,
    viewModel: StudyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val speaker = rememberSpeaker()

    LaunchedEffect(mode) { viewModel.start(mode) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部：进度 + 退出
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "退出")
            }
            Text(
                text = if (mode == SessionMode.NEW) "学习新词" else "复习",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (!state.loading && !state.empty && !state.finished) {
                Text(
                    text = (state.index + 1).toString() + " / " + state.total,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        when {
            state.loading -> LoadingBox()
            state.empty -> EmptyBox(
                message = if (mode == SessionMode.NEW) "今天的新词已经学完了" else "现在没有到期的复习卡",
                onBack = onBack,
            )
            state.finished -> EmptyBox(message = "本轮完成，共 " + state.total + " 个词", onBack = onBack)
            state.current != null -> {
                Box(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    StudyCardView(
                        word = state.current!!.word,
                        flipped = state.flipped,
                        showBackContent = state.flipped,
                        onFlip = viewModel::flip,
                        onSpeak = speaker::speak,
                    )
                }

                state.error?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                Spacer(Modifier.height(12.dp))
                if (state.flipped) {
                    RatingRow(previews = state.previews, onRate = viewModel::rate)
                } else {
                    val isReview = mode == SessionMode.REVIEW
                    Button(
                        onClick = viewModel::flip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Text(if (isReview) "显示答案" else "查看释义")
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun RatingRow(previews: Map<Rating, Long>, onRate: (Rating) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RatingButton("重来", Rating.AGAIN, Color(0xFFD1495B), previews[Rating.AGAIN], onRate, Modifier.weight(1f))
        RatingButton("困难", Rating.HARD, Color(0xFFE08A3C), previews[Rating.HARD], onRate, Modifier.weight(1f))
        RatingButton("良好", Rating.GOOD, Color(0xFF2E8B6E), previews[Rating.GOOD], onRate, Modifier.weight(1f))
        RatingButton("简单", Rating.EASY, Color(0xFF3A78C2), previews[Rating.EASY], onRate, Modifier.weight(1f))
    }
}

@Composable
private fun RatingButton(
    label: String,
    rating: Rating,
    color: Color,
    intervalMs: Long?,
    onRate: (Rating) -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = { onRate(rating) },
        modifier = modifier.height(60.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (intervalMs != null) {
                Text(
                    text = formatInterval(intervalMs),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyBox(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onBack) { Text("返回首页") }
    }
}
