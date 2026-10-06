package com.wordbook.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordbook.BuildConfig
import com.wordbook.domain.model.ArticleStyle
import com.wordbook.domain.model.DarkModeSetting
import com.wordbook.domain.model.UiStyle
import com.wordbook.ui.components.StyleSectionTitle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // 选一个备份文件（.json），选完先确认再导入
    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) pendingImportUri = uri }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(Unit) { viewModel.loadStats() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        state.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }

        SectionCard("界面风格") {
            Text(
                text = "点一下立即生效，整套界面（配色 / 圆角 / 字体 / 卡片样式）都会跟着换",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            UiStyle.entries.forEach { style ->
                StyleOptionRow(
                    style = style,
                    selected = state.settings.uiStyle == style,
                    onClick = { viewModel.setUiStyle(style) },
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("深浅模式", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (state.settings.uiStyle == UiStyle.MIDNIGHT) {
                    "当前选的「暗夜专注」本身就是深色风格，固定深色"
                } else {
                    "墨纸 / 薄荷圆润 / 马克笔 都各自有浅色与深色两套"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkModeSetting.entries.forEach { mode ->
                    FilterChip(
                        selected = state.settings.darkMode == mode,
                        onClick = { viewModel.setDarkMode(mode) },
                        label = { Text(mode.label) },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionCard("学习计划") {
            NumberRow(
                label = "每日新词数",
                value = state.settings.dailyNewWords,
                onChange = viewModel::setDailyNewWords,
            )
            NumberRow(
                label = "每日复习上限（0 = 不限）",
                value = state.settings.dailyReviewLimit,
                min = 0,
                onChange = viewModel::setDailyReviewLimit,
            )
            SwitchRow(
                label = "新词乱序",
                checked = state.settings.shuffleNewWords,
                onChange = viewModel::setShuffleNewWords,
                hint = if (state.settings.shuffleNewWords) "当前：乱序（KaoYanluan_1 + 稳定乱序键）" else "当前：按词书顺序（KaoYan_2 → KaoYan_3）",
            )
        }

        Spacer(Modifier.height(12.dp))
        SectionCard("文章") {
            Text("文章风格", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArticleStyle.entries.forEach { style ->
                    FilterChip(
                        selected = state.settings.articleStyle == style,
                        onClick = { viewModel.setArticleStyle(style) },
                        label = { Text(style.label) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "每天用学过的词生成文章，每篇 " + 5 + "–20 个目标词；今天学的词少于 5 个时不能生成",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))
        SectionCard("DeepSeek API") {
            OutlinedTextField(
                value = state.apiKeyInput,
                onValueChange = viewModel::onApiKeyChange,
                label = { Text("API Key") },
                singleLine = true,
                visualTransformation = if (state.apiKeyVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::toggleApiKeyVisible) {
                    Text(if (state.apiKeyVisible) "隐藏" else "显示")
                }
                Button(onClick = viewModel::saveApiKey) { Text("保存") }
                OutlinedButton(onClick = viewModel::testConnection, enabled = !state.testing) {
                    Text(if (state.testing) "测试中…" else "测试连接")
                }
            }
            state.testResult?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Key 只保存在本机 DataStore，不会写死在源码里，也不会上传；发给 DeepSeek 的内容只有单词和释义",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))
        SectionCard("每日提醒") {
            SwitchRow(
                label = "开启每日提醒",
                checked = state.settings.reminderEnabled,
                onChange = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    viewModel.setReminderEnabled(enabled)
                },
                hint = "时间：" + pad(state.settings.reminderHour) + ":" + pad(state.settings.reminderMinute),
            )
            TextButton(onClick = { showTimeDialog = true }) { Text("修改提醒时间") }
        }

        Spacer(Modifier.height(12.dp))
        SectionCard("高级") {
            Text(
                text = "目标记忆保持率：" + String.format(java.util.Locale.US, "%.2f", state.settings.desiredRetention),
                style = MaterialTheme.typography.bodyLarge,
            )
            Slider(
                value = state.settings.desiredRetention,
                onValueChange = { viewModel.setDesiredRetention(it) },
                valueRange = 0.70f..0.98f,
                steps = 27,
            )
            Text(
                text = "保持率越高，复习越频繁。默认 0.90 是 FSRS 官方推荐值",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))
        SectionCard("数据统计") {
            StatLine("累计学过的词", state.learnedTotal.toString())
            StatLine("连续打卡", state.streak.toString() + " 天")
            StatLine("今日新词 / 复习", state.todayNew.toString() + " / " + state.todayReview)
            StatLine("离线词典收录", state.dictSize.toString() + " 条")
        }

        Spacer(Modifier.height(12.dp))
        SectionCard("数据导出与清空") {
            Button(onClick = viewModel::exportData, enabled = !state.exporting) {
                Text(if (state.exporting) "导出中…" else "导出学习数据（JSON）")
            }
            state.exportPath?.let { path ->
                Spacer(Modifier.height(8.dp))
                Text(path, style = MaterialTheme.typography.bodyMedium)
                val intent = remember(path) { viewModel.shareIntentFor(path) }
                if (intent != null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { context.startActivity(Intent.createChooser(intent, "分享导出文件")) }) {
                        Text("分享文件")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { importPicker.launch(arrayOf("*/*")) },
                enabled = !state.importing && !state.clearing,
            ) {
                Text(if (state.importing) "导入中…" else "导入学习数据（换手机迁移）")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "导入会先清空当前学习数据再写入备份；词库与设置（含 API Key）不受影响。建议先导出一份再导入。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { showClearDialog = true }, enabled = !state.clearing) {
                Text("清空全部数据")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "清空会删除学习进度、复习日志、文章与设置（含 API Key），词库会在下次启动时重新导入",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))
        // 版本号必须从构建产物里读，不要写死字符串：
        // 之前这里硬编码了 "v1.0"，结果 APK 已经发到 1.0.2 了，界面上还显示 v1.0
        Text(
            text = "背单词 v" + BuildConfig.VERSION_NAME + "（build " + BuildConfig.VERSION_CODE + "）",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "个人自用 · 数据全部保存在本机 · 词库 5046 词",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("确认清空全部数据？") },
            text = { Text("学习进度、复习日志和文章都会被删除，且无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearAllData()
                }) { Text("确认清空") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            },
        )
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("确认导入备份？") },
            text = {
                Text("当前的学习进度、复习日志和文章会被**备份文件里的内容覆盖**，无法撤销。\n\n如果还没导出过当前进度，建议先点上面的「导出学习数据」。")
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportUri = null
                    viewModel.importData(uri)
                }) { Text("确认导入") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("取消") }
            },
        )
    }

    if (showTimeDialog) {
        var hour by remember { mutableStateOf(state.settings.reminderHour.toString()) }
        var minute by remember { mutableStateOf(state.settings.reminderMinute.toString()) }
        AlertDialog(
            onDismissRequest = { showTimeDialog = false },
            title = { Text("提醒时间") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { hour = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("时") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(90.dp),
                        singleLine = true,
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { minute = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("分") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(90.dp),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showTimeDialog = false
                    viewModel.setReminderTime(
                        hour.toIntOrNull()?.coerceIn(0, 23) ?: 20,
                        minute.toIntOrNull()?.coerceIn(0, 59) ?: 0,
                    )
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showTimeDialog = false }) { Text("取消") } },
        )
    }
}

/** 一行风格选项：色块预览 + 名称 + 说明 + 选中标记 */
@Composable
private fun StyleOptionRow(style: UiStyle, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            style.swatch.forEach { value ->
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(value))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = style.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            Text(
                text = style.slogan,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .then(
                    if (selected) {
                        Modifier.background(MaterialTheme.colorScheme.primary)
                    } else {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Text("✓", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            StyleSectionTitle(title)
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun NumberRow(
    label: String,
    value: Int,
    min: Int = 1,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { onChange((value - step(value)).coerceAtLeast(min)) }) { Text("-") }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        OutlinedButton(onClick = { onChange(value + step(value)) }) { Text("+") }
    }
}

private fun step(value: Int): Int = if (value >= 100) 10 else if (value >= 20) 5 else 1

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    hint: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

private fun pad(value: Int): String = if (value < 10) "0" + value else value.toString()
