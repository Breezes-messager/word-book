package com.wordbook.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 搜索结果点开后的底部词义卡。
 * 收起状态 = 渲染图里那张（释义 + 英释 + 记忆法 + 两个按钮）；
 * 点「查看完整词卡」展开出例句 / 同近义 / 同根词 / 短语。
 */
@Composable
fun WordDetailContent(
    detail: WordDetail,
    full: Boolean,
    added: Boolean,
    onToggleFull: () -> Unit,
    onAdd: () -> Unit,
    onSpeak: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 620.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = detail.headword,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            detail.phonetic?.let {
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(
                    text = "/" + it + "/",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.padding(horizontal = 4.dp))
            OutlinedButton(onClick = onSpeak) { Text("▶ 发音") }
        }

        detail.badge?.let {
            Spacer(Modifier.height(6.dp))
            Text(
                text = it.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(10.dp))
        detail.translationLines.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 1.dp),
            )
        }

        if (detail.definitionLines.isNotEmpty()) {
            SectionLabel("英释")
            detail.definitionLines.take(if (full) 6 else 2).forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 1.dp),
                )
            }
        }

        detail.remMethod?.let {
            SectionLabel("记忆法")
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        if (full) {
            if (detail.examples.isNotEmpty()) {
                SectionLabel("例句")
                detail.examples.take(3).forEach { (en, cn) ->
                    Text(en, style = MaterialTheme.typography.bodyMedium)
                    if (cn.isNotBlank()) {
                        Text(
                            cn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            if (detail.synonyms.isNotEmpty()) {
                SectionLabel("同近义")
                detail.synonyms.forEach { (sense, words) ->
                    Text(
                        sense,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(words.joinToString(" / "), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                }
            }
            if (detail.relatedWords.isNotEmpty()) {
                SectionLabel("同根词")
                detail.relatedWords.take(6).forEach { (word, cn) ->
                    Text(
                        text = word + "  " + cn,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (detail.phrases.isNotEmpty()) {
                SectionLabel("短语")
                detail.phrases.take(4).forEach { (en, cn) ->
                    Text(
                        text = en + "  " + cn,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onAdd,
            enabled = !added,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (added) "已加入今日学习" else "加入今日学习")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onToggleFull, modifier = Modifier.fillMaxWidth()) {
            Text(if (full) "收起" else "查看完整词卡")
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(10.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 13.sp,
    )
    Spacer(Modifier.height(4.dp))
}
