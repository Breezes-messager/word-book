package com.wordbook.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wordbook.domain.fsrs.Rating
import com.wordbook.ui.theme.LocalAppStyle
import com.wordbook.ui.theme.SectionTitle
import com.wordbook.util.formatInterval
import androidx.compose.ui.unit.sp

/**
 * 风格化卡片：不同风格对应不同圆角 / 描边 / 阴影。
 * 马克笔风是「偏移的黑色实心块」硬阴影，Compose 自带的模糊阴影做不出这个效果。
 */
@Composable
fun StyleCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalAppStyle.current
    val shape = RoundedCornerShape(tokens.cardCorner.dp)
    val inner = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(containerColor)
        .then(if (tokens.cardBorder != null) Modifier.border(tokens.cardBorder, shape) else Modifier)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)

    Box(modifier = modifier) {
        if (tokens.cardHardShadow) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(x = 4.dp, y = 4.dp)
                    .clip(shape)
                    .background(tokens.hardShadowColor),
            )
        }
        Column(modifier = inner.padding(16.dp), content = content)
    }
}

/**
 * 风格化区块标题（今日任务 / 词书预览 / 设置分组…）。
 * 这是各风格辨识度最高的地方：墨纸加细线、暗夜拉字距、马克笔套荧光框。
 */
@Composable
fun StyleSectionTitle(text: String, modifier: Modifier = Modifier) {
    val tokens = LocalAppStyle.current
    when (tokens.sectionTitle) {
        SectionTitle.RULE -> Column(modifier) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = tokens.titleFont,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline),
            )
        }

        SectionTitle.WIDE -> Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Light,
            letterSpacing = 6.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )

        SectionTitle.BOX -> Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = modifier
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(2.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        )

        SectionTitle.PLAIN -> Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = tokens.titleWeight,
            modifier = modifier,
        )
    }
}

/** 风格化进度条：细线（墨纸 / 暗夜）、圆角粗条（薄荷）、黑边黄条（马克笔） */
@Composable
fun StyleProgress(progress: Float, modifier: Modifier = Modifier) {
    val tokens = LocalAppStyle.current
    val shape = if (tokens.progressRounded) RoundedCornerShape(percent = 50) else RoundedCornerShape(0.dp)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = tokens.progressColor ?: MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(tokens.progressHeight.dp)
            .clip(shape)
            .background(track)
            .then(if (tokens.progressBorder != null) Modifier.border(tokens.progressBorder, shape) else Modifier),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(fill),
        )
    }
}

/** 风格化四档评分按钮，按钮上带 FSRS 预估间隔 */
@Composable
fun StyleRatingRow(
    previews: Map<Rating, Long>,
    onRate: (Rating) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppStyle.current
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Rating.entries.forEachIndexed { index, rating ->
            val style = tokens.rating.getOrElse(index) { tokens.rating.last() }
            RatingChip(
                label = labelOf(rating),
                interval = previews[rating],
                style = style,
                onClick = { onRate(rating) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RowScope.RatingChip(
    label: String,
    interval: Long?,
    style: com.wordbook.ui.theme.RatingButtonStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(LocalAppStyle.current.ratingCorner.dp)
    Box(modifier = modifier) {
        if (style.hardShadow) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(x = 3.dp, y = 3.dp)
                    .clip(shape)
                    .background(LocalAppStyle.current.hardShadowColor),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(style.background)
                .then(
                    if (style.bordered && style.borderColor != Color.Unspecified) {
                        Modifier.border(BorderStroke(2.dp, style.borderColor), shape)
                    } else {
                        Modifier
                    }
                )
                .clickable { onClick() }
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = style.content,
            )
            if (interval != null) {
                Text(
                    text = formatInterval(interval),
                    style = MaterialTheme.typography.bodyMedium,
                    color = style.content.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun labelOf(rating: Rating): String = when (rating) {
    Rating.AGAIN -> "重来"
    Rating.HARD -> "困难"
    Rating.GOOD -> "良好"
    Rating.EASY -> "简单"
}
