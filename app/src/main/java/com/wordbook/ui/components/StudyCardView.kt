package com.wordbook.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wordbook.data.db.WordEntity
import com.wordbook.domain.fsrs.Rating
import com.wordbook.data.repo.examples
import com.wordbook.data.repo.phrases
import com.wordbook.data.repo.relatedWords
import com.wordbook.data.repo.synonyms
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
    /** 这个词在自己生成的文章里出现过的句子 */
    contexts: List<String> = emptyList(),
    /** 翻面后四方向滑动评分：左=重来 下=困难 右=良好 上=简单 */
    onRateByGesture: ((Rating) -> Unit)? = null,
) {
    // key 换成 word.id：换到下一张卡时动画状态重置，直接以正面出现，
    // 不会播一段 180°→0° 的"翻回来"动画（那个动画容易被误解成"回到上一张"）
    val rotation by key(word.id) {
        animateFloatAsState(
            targetValue = if (flipped) 180f else 0f,
            animationSpec = tween(320),
            label = "cardRotation",
        )
    }
    var dragOffset by remember(word.id) { mutableStateOf(Offset.Zero) }
    // 64dp：比原来的 90dp 更容易触发，短促的滑动手势也能识别
    val threshold = with(LocalDensity.current) { 64.dp.toPx() }
    // 释义内容可能需要上下滚动：能滚的时候上下滑交给滚动，不能滚的时候上下滑才是评分
    val scrollState = rememberScrollState()
    val verticalRatingEnabled = showBackContent && scrollState.maxValue == 0

    val ratingOf = { dx: Float, dy: Float, limit: Float ->
        if (verticalRatingEnabled || kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
            ratingFor(dx, dy, limit)
        } else {
            null
        }
    }
    val gestureHint = if (showBackContent && onRateByGesture != null) {
        ratingOf(dragOffset.x, dragOffset.y, threshold * 0.6f)
    } else {
        null
    }

    val tokens = LocalAppStyle.current
    val shape = RoundedCornerShape(tokens.cardCorner.dp)

    // 手势必须放在**未旋转**的外层：
    // 卡片翻面后是 rotationY = 180°，Compose 的指针坐标会跟着左右镜像，
    // 放在里面的话"物理右滑"在局部坐标里是左滑，会被判成「重来」。
    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .pointerInput(word.id, showBackContent, onRateByGesture) {
                detectDragGestures(
                    onDragEnd = {
                        val rating = if (showBackContent) {
                            ratingOf(dragOffset.x, dragOffset.y, threshold)
                        } else {
                            null
                        }
                        when {
                            rating != null -> onRateByGesture?.invoke(rating)
                            // 未翻面时右滑翻面（原有交互）
                            !showBackContent && dragOffset.x > threshold -> onFlip()
                        }
                        dragOffset = Offset.Zero
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        dragOffset += delta
                    },
                )
            },
    ) {
        // 真正会翻转的那一层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
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
                    BackContent(
                        word = word,
                        onSpeak = onSpeak,
                        contexts = contexts,
                        scrollState = scrollState,
                    )
                } else {
                    FrontContent(word = word, onSpeak = onSpeak, hint = if (flipped) "" else "点击或右滑翻面")
                }
            }
        }

        // 拖动时的方向提示（外层没旋转，直接画即可）
        if (gestureHint != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = gestureLabel(gestureHint),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                )
            }
        }
    }
}

/** 根据拖动方向判断要打的分：右滑 = 良好，左滑 = 重来 */
private fun ratingFor(dx: Float, dy: Float, threshold: Float): Rating? {
    if (kotlin.math.abs(dx) < threshold && kotlin.math.abs(dy) < threshold) return null
    return if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
        if (dx > 0) Rating.GOOD else Rating.AGAIN
    } else {
        if (dy > 0) Rating.HARD else Rating.EASY
    }
}

private fun gestureLabel(rating: Rating): String = when (rating) {
    Rating.AGAIN -> "重来"
    Rating.HARD -> "困难"
    Rating.GOOD -> "良好"
    Rating.EASY -> "简单"
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
private fun BackContent(
    word: WordEntity,
    onSpeak: (String) -> Unit,
    contexts: List<String>,
    scrollState: ScrollState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
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

        // 记忆法：词根词缀拆解（约 45% 的词有）
        word.remMethod?.takeIf { it.isNotBlank() }?.let { method ->
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("记忆法", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.height(4.dp))
            Text(
                text = method,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        // 同近义词（95% 的词有）
        val synonyms = word.synonyms()
        if (synonyms.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("同近义", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            synonyms.forEach { group ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildString {
                        if (group.pos.isNotBlank()) append(group.pos).append(". ")
                        append(group.words.joinToString(" / "))
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        // 同根词（83% 的词有）
        val related = word.relatedWords()
        if (related.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("同根词", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            related.forEach { group ->
                group.words.forEach { item ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            if (group.pos.isNotBlank()) append(group.pos).append(". ")
                            append(item.hwd)
                            if (item.tran.isNotBlank()) append("  ").append(item.tran)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        // 文章中的用法（用自己生成的句子回填，比词典例句更贴记忆）
        if (contexts.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                "文章中的用法",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            contexts.forEach { sentence ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = sentence,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
