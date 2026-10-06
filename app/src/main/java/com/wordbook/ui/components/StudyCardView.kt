package com.wordbook.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wordbook.data.db.WordEntity
import com.wordbook.data.repo.examples
import com.wordbook.data.repo.phrases
import com.wordbook.data.repo.translationLines
import com.wordbook.ui.theme.LocalAppStyle

/**
 * 学习卡片：
 *  正面 —— 只显示单词 + 音标 + 发音按钮
 *  背面 —— 中文释义 + 英释 + 例句 + 短语
 * 点击或向右滑动翻面。
 */
@Composable
fun StudyCardView(
    word: WordEntity,
    flipped: Boolean,
    showBackContent: Boolean,
    onFlip: () -> Unit,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(320),
        label = "cardRotation",
    )
    var dragAccum by remember(word.id) { mutableFloatStateOf(0f) }
    val threshold = with(LocalDensity.current) { 90.dp.toPx() }

    val tokens = LocalAppStyle.current
    val shape = RoundedCornerShape(tokens.cardCorner.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .pointerInput(word.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragAccum > threshold) onFlip()
                        dragAccum = 0f
                    },
                    onHorizontalDrag = { _, delta -> dragAccum += delta },
                )
            }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (tokens.cardBorder != null) Modifier.border(tokens.cardBorder, shape) else Modifier)
            .clickable { onFlip() },
    ) {
        // 背面内容需要再翻 180 度，否则文字是镜像的
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { if (flipped) rotationY = 180f },
        ) {
            if (showBackContent) {
                BackContent(word = word, onSpeak = onSpeak)
            } else {
                FrontContent(word = word, onSpeak = onSpeak, hint = if (flipped) "" else "点击或右滑翻面")
            }
        }
    }
}

@Composable
private fun FrontContent(word: WordEntity, onSpeak: (String) -> Unit, hint: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = word.headword,
            style = MaterialTheme.typography.displaySmall,
            fontFamily = LocalAppStyle.current.wordFont,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        val phonetic = word.phoneticUs ?: word.phoneticUk
        if (!phonetic.isNullOrBlank()) {
            Text(text = "/" + phonetic + "/", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(8.dp))
        IconButton(onClick = { onSpeak(word.headword) }) {
            Icon(Icons.Default.PlayArrow, contentDescription = "发音")
        }
        if (hint.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BackContent(word: WordEntity, onSpeak: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            text = word.headword,
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = LocalAppStyle.current.wordFont,
            fontWeight = FontWeight.Bold,
        )
        val phonetic = word.phoneticUs ?: word.phoneticUk
        if (!phonetic.isNullOrBlank()) {
            Text(text = "/" + phonetic + "/", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(12.dp))

        word.translationLines().forEach { line ->
            Text(text = line, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
        }

        val english = word.transEn?.takeIf { it.isNotBlank() }
        if (english != null) {
            Spacer(Modifier.height(8.dp))
            Text("英释", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(text = english, style = MaterialTheme.typography.bodyMedium)
        }

        val examples = word.examples()
        if (examples.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("例句", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            examples.forEach { pair ->
                Spacer(Modifier.height(6.dp))
                Text(text = pair.en, style = MaterialTheme.typography.bodyLarge)
                if (pair.cn.isNotBlank()) {
                    Text(
                        text = pair.cn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        val phraseList = word.phrases()
        if (phraseList.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("短语", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            phraseList.forEach { pair ->
                Spacer(Modifier.height(6.dp))
                Text(text = pair.en, style = MaterialTheme.typography.bodyLarge)
                if (pair.cn.isNotBlank()) {
                    Text(
                        text = pair.cn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = { onSpeak(word.headword) }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "发音")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
