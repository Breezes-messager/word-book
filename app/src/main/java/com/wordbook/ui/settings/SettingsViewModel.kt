package com.wordbook.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordbook.data.dict.DictRepository
import com.wordbook.data.prefs.SettingsRepository
import com.wordbook.data.remote.ArticleGenerator
import com.wordbook.data.repo.StatsRepository
import com.wordbook.data.repo.StudyRepository
import com.wordbook.domain.model.AppSettings
import com.wordbook.domain.model.ArticleStyle
import com.wordbook.util.DataExporter
import com.wordbook.work.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val loading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val apiKeyInput: String = "",
    val apiKeyVisible: Boolean = false,
    val testing: Boolean = false,
    val testResult: String? = null,
    val message: String? = null,
    val error: String? = null,
    val learnedTotal: Int = 0,
    val streak: Int = 0,
    val todayNew: Int = 0,
    val todayReview: Int = 0,
    val dictSize: Int = 0,
    val exportPath: String? = null,
    val exporting: Boolean = false,
    val clearing: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val statsRepository: StatsRepository,
    private val studyRepository: StudyRepository,
    private val dictRepository: DictRepository,
    private val articleGenerator: ArticleGenerator,
    private val reminderScheduler: ReminderScheduler,
    private val dataExporter: DataExporter,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { current ->
                    current.copy(
                        loading = false,
                        settings = settings,
                        apiKeyInput = if (current.apiKeyInput.isEmpty()) settings.apiKey else current.apiKeyInput,
                    )
                }
            }
        }
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            val task = runCatching { studyRepository.todayTask() }.getOrNull()
            _state.update {
                it.copy(
                    learnedTotal = runCatching { statsRepository.learnedTotal() }.getOrDefault(0),
                    streak = runCatching { statsRepository.streak() }.getOrDefault(0),
                    todayNew = task?.newDone ?: 0,
                    todayReview = task?.reviewDone ?: 0,
                    dictSize = runCatching { dictRepository.size() }.getOrDefault(0),
                )
            }
        }
    }

    fun setDailyNewWords(value: Int) {
        viewModelScope.launch { settingsRepository.setDailyNewWords(value) }
    }

    fun setDailyReviewLimit(value: Int) {
        viewModelScope.launch { settingsRepository.setDailyReviewLimit(value) }
    }

    fun setShuffleNewWords(value: Boolean) {
        viewModelScope.launch { settingsRepository.setShuffleNewWords(value) }
    }

    fun setArticleStyle(style: ArticleStyle) {
        viewModelScope.launch { settingsRepository.setArticleStyle(style) }
    }

    fun setDesiredRetention(value: Float) {
        viewModelScope.launch { settingsRepository.setDesiredRetention(value) }
    }

    fun onApiKeyChange(value: String) = _state.update { it.copy(apiKeyInput = value, testResult = null) }

    fun toggleApiKeyVisible() = _state.update { it.copy(apiKeyVisible = !it.apiKeyVisible) }

    fun saveApiKey() {
        viewModelScope.launch {
            settingsRepository.setApiKey(_state.value.apiKeyInput)
            _state.update { it.copy(message = "API Key 已保存到本机（不会上传、不会提交到 git）") }
        }
    }

    fun testConnection() {
        val key = _state.value.apiKeyInput.ifBlank { _state.value.settings.apiKey }
        if (key.isBlank()) {
            _state.update { it.copy(testResult = "请先填写 API Key") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(testing = true, testResult = "正在测试…") }
            val result = articleGenerator.testConnection(key)
            result.fold(
                onSuccess = { reply ->
                    settingsRepository.setApiKey(key)
                    _state.update { it.copy(testing = false, testResult = "连接成功：模型回复「" + reply + "」") }
                },
                onFailure = { error ->
                    _state.update { it.copy(testing = false, testResult = "连接失败：" + (error.message ?: "未知错误")) }
                },
            )
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReminderEnabled(enabled)
            val settings = settingsRepository.current()
            reminderScheduler.apply(enabled, settings.reminderHour, settings.reminderMinute)
            _state.update {
                it.copy(message = if (enabled) "已开启每日提醒" else "已关闭每日提醒")
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(hour, minute)
            val settings = settingsRepository.current()
            reminderScheduler.apply(settings.reminderEnabled, hour, minute)
            _state.update { it.copy(message = "提醒时间已设置为 " + pad(hour) + ":" + pad(minute)) }
        }
    }

    fun exportData() {
        viewModelScope.launch {
            _state.update { it.copy(exporting = true, error = null) }
            runCatching { dataExporter.export() }.fold(
                onSuccess = { file ->
                    _state.update {
                        it.copy(exporting = false, exportPath = file.absolutePath, message = "已导出到：" + file.absolutePath)
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(exporting = false, error = "导出失败：" + (error.message ?: "")) }
                },
            )
        }
    }

    /** 供界面分享导出文件 */
    fun shareIntentFor(path: String) = runCatching { dataExporter.shareIntent(File(path)) }.getOrNull()

    fun clearAllData() {
        viewModelScope.launch {
            _state.update { it.copy(clearing = true) }
            runCatching { dataExporter.clearAll() }.fold(
                onSuccess = {
                    _state.update {
                        it.copy(clearing = false, message = "已清空全部学习数据，词库会在下次启动时重新导入")
                    }
                    loadStats()
                },
                onFailure = { error ->
                    _state.update { it.copy(clearing = false, error = "清空失败：" + (error.message ?: "")) }
                },
            )
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null, error = null) }

    private fun pad(value: Int): String = if (value < 10) "0" + value else value.toString()
}
