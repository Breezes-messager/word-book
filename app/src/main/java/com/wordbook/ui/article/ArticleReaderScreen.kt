package com.wordbook.ui.article

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordbook.ui.dict.DictBottomSheet
import com.wordbook.ui.dict.DictSheetViewModel
import com.wordbook.util.rememberSpeaker

/**
 * 历史文章阅读器。
 * 从「统计 → 最近的文章」或文章页的「历史文章」点进来，读任意一篇已经存下来的文章。
 */
@Composable
fun ArticleReaderScreen(
    articleId: Long,
    onBack: () -> Unit,
    viewModel: ArticleReaderViewModel = hiltViewModel(),
    dictViewModel: DictSheetViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dictState by dictViewModel.state.collectAsStateWithLifecycle()
    val speaker = rememberSpeaker()
    var translationExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(articleId) { viewModel.load(articleId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onBack) { Text("← 返回") }
            Text(
                text = state.dateKey,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))

        when {
            state.loading -> CircularProgressIndicator()
            state.content != null -> {
                ArticleBody(
                    content = state.content!!,
                    translationExpanded = translationExpanded,
                    onToggleTranslation = { translationExpanded = !translationExpanded },
                    onWordClick = dictViewModel::lookup,
                    fontScale = state.fontScale,
                    lineHeightScale = state.lineHeightScale,
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    text = buildFooter(state.dateKey, state.createdAt, state.isSample, state.style),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                Text(
                    text = state.error ?: "这篇文章读不出来了",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onBack) { Text("返回") }
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    dictState?.let { sheetState ->
        DictBottomSheet(
            state = sheetState,
            onDismiss = dictViewModel::dismiss,
            onSpeak = speaker::speak,
            onSelectWord = dictViewModel::lookup,
        )
    }
}
