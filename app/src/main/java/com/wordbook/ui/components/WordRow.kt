package com.wordbook.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wordbook.data.db.WordEntity
import com.wordbook.data.repo.translationLines

/** 词书列表里的一行：单词 + 音标 + 中文释义首行 + 发音 */
@Composable
fun WordRow(
    word: WordEntity,
    modifier: Modifier = Modifier,
    onClick: ((WordEntity) -> Unit)? = null,
    onSpeak: ((String) -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke(word) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = word.headword,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            val phonetic = word.phoneticUs ?: word.phoneticUk
            if (!phonetic.isNullOrBlank()) {
                Text(
                    text = "/" + phonetic + "/",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val meaning = word.translationLines().firstOrNull()
            if (!meaning.isNullOrBlank()) {
                Text(
                    text = meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (onSpeak != null) {
            IconButton(onClick = { onSpeak(word.headword) }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "发音")
            }
        }
    }
}
