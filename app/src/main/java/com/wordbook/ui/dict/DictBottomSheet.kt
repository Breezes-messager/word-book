package com.wordbook.ui.dict

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 点击单词后弹出的离线词典卡片 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictBottomSheet(
    state: DictSheetUiState,
    onDismiss: () -> Unit,
    onSpeak: (String) -> Unit,
    onSelectWord: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (state.loading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }
                return@Column
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.headline,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    val phonetic = state.phonetic
                    if (!phonetic.isNullOrBlank()) {
                        Text(
                            text = "/" + phonetic + "/",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = { onSpeak(state.headline) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "发音")
                }
            }

            state.lemmaNote?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "词形还原：" + it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (state.notFound) {
                Spacer(Modifier.height(12.dp))
                Text("词典未收录这个词", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "本地词典收录了考研 / 四六级 / 高考词汇及其词形变体",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.translation?.takeIf { it.isNotBlank() }?.let { translation ->
                Spacer(Modifier.height(12.dp))
                Text("中文释义", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(translation, style = MaterialTheme.typography.bodyLarge)
            }

            state.definition?.takeIf { it.isNotBlank() }?.let { definition ->
                Spacer(Modifier.height(12.dp))
                Text("英释", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(definition, style = MaterialTheme.typography.bodyMedium)
            }

            if (state.suggestions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("你是不是想查：", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                state.suggestions.forEach { word ->
                    Text(
                        text = word,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectWord(word) }
                            .padding(vertical = 6.dp),
                    )
                }
            }

            if (state.examples.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text("例句", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                state.examples.forEach { pair ->
                    Spacer(Modifier.height(6.dp))
                    Text(pair.en, style = MaterialTheme.typography.bodyLarge)
                    if (pair.cn.isNotBlank()) {
                        Text(
                            pair.cn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (state.phrases.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text("短语", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                state.phrases.forEach { pair ->
                    Spacer(Modifier.height(6.dp))
                    Text(pair.en, style = MaterialTheme.typography.bodyLarge)
                    if (pair.cn.isNotBlank()) {
                        Text(
                            pair.cn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "释义来自本地词典，查词不联网",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
