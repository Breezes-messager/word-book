package com.wordbook.ui.words

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordbook.ui.components.WordRow
import com.wordbook.ui.dict.DictBottomSheet
import com.wordbook.ui.dict.DictSheetViewModel
import com.wordbook.util.rememberSpeaker

@Composable
fun WordListScreen(
    viewModel: WordListViewModel = hiltViewModel(),
    dictViewModel: DictSheetViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dictState by dictViewModel.state.collectAsStateWithLifecycle()
    val speaker = rememberSpeaker()
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.words.size - 10
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMore() }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            label = { Text("搜索单词（前缀匹配）") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = "词库共 " + state.total + " 个词，当前显示 " + state.words.size + " 个",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(state.words, key = { it.id }) { word ->
                WordRow(
                    word = word,
                    onClick = { dictViewModel.lookup(it.headword) },
                    onSpeak = speaker::speak,
                )
                HorizontalDivider()
            }
            if (state.loading) {
                item { CircularProgressIndicator(modifier = Modifier.padding(16.dp)) }
            }
        }
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
