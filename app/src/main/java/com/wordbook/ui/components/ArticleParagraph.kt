package com.wordbook.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.wordbook.domain.lemmatize.WordNormalizer
import com.wordbook.ui.theme.LocalAppStyle

private val wordRegex = Regex("" + "[A-Za-z][A-Za-z'’-]*")

/**
 * 文章正文段落：每个英文单词都可以点击查词，目标词高亮。
 * 点击位置先经 TextLayoutResult 换算成字符下标，再映射回单词。
 */
@Composable
fun ArticleParagraph(
    text: String,
    highlights: Set<String>,
    onWordClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val tokens = LocalAppStyle.current
    val highlighted = tokens.highlightText ?: MaterialTheme.colorScheme.primary
    val background = tokens.highlightBackground
    val highlightStyle = SpanStyle(
        color = highlighted,
        background = background ?: androidx.compose.ui.graphics.Color.Unspecified,
        fontWeight = FontWeight.Bold,
        // 有底色（薄荷 / 马克笔）时不需要下划线，纸感风格用下划线代替底色
        textDecoration = if (background == null) TextDecoration.Underline else TextDecoration.None,
    )

    val prepared = remember(text, highlights) {
        val ranges = mutableListOf<Triple<Int, Int, String>>()
        val builder = buildAnnotatedString {
            var cursor = 0
            wordRegex.findAll(text).forEach { match ->
                append(text.substring(cursor, match.range.first))
                val word = match.value
                ranges += Triple(match.range.first, match.range.last, word)
                if (highlights.contains(WordNormalizer.normalize(word))) {
                    withStyle(highlightStyle) { append(word) }
                } else {
                    append(word)
                }
                cursor = match.range.last + 1
            }
            if (cursor < text.length) append(text.substring(cursor))
        }
        builder to ranges
    }

    val annotated = prepared.first
    val ranges = prepared.second
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotated,
        style = style,
        onTextLayout = { layout = it },
        modifier = modifier.pointerInput(text, ranges) {
            detectTapGestures { position ->
                val offset = layout?.getOffsetForPosition(position) ?: return@detectTapGestures
                val hit = ranges.firstOrNull { offset >= it.first && offset <= it.second }
                if (hit != null) onWordClick(hit.third)
            }
        },
    )
}
