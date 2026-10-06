package com.wordbook.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

/**
 * 把「字号倍率 + 行距倍率」换算成实际的 TextStyle。
 *
 * 注意行高要跟着字号一起放大（否则字变大后行与行会挤在一起），
 * 再乘上行距倍率做额外的疏密调节。
 */
fun readingStyle(base: TextStyle, fontScale: Float, lineHeightScale: Float): TextStyle {
    val size = base.fontSize * fontScale
    val lineHeight = base.lineHeight * fontScale * lineHeightScale
    return base.copy(fontSize = size, lineHeight = lineHeight)
}

/** 标题跟着放大，但只跟一半，避免标题过大把正文挤下去 */
fun readingTitleStyle(base: TextStyle, fontScale: Float): TextStyle {
    val damped = 1f + (fontScale - 1f) * 0.5f
    return base.copy(
        fontSize = base.fontSize * damped,
        lineHeight = base.lineHeight * (1f + (fontScale - 1f) * 0.3f),
    )
}

/** 当前设置的阅读样式（正文字号） */
@Composable
fun articleBodyStyle(fontScale: Float, lineHeightScale: Float): TextStyle =
    readingStyle(MaterialTheme.typography.bodyLarge, fontScale, lineHeightScale)

/** 中文翻译 / 次要文字照样放大，但比正文小一档 */
@Composable
fun articleSecondaryStyle(fontScale: Float, lineHeightScale: Float): TextStyle =
    readingStyle(MaterialTheme.typography.bodyMedium, fontScale, lineHeightScale)
