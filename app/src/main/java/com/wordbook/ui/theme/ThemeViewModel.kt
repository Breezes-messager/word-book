package com.wordbook.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordbook.data.prefs.SettingsRepository
import com.wordbook.domain.model.DarkModeSetting
import com.wordbook.domain.model.UiStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** 主题设置（界面风格 + 深浅偏好），改了立即生效 */
data class ThemeState(
    val style: UiStyle = UiStyle.SYSTEM,
    val darkMode: DarkModeSetting = DarkModeSetting.FOLLOW_SYSTEM,
)

@HiltViewModel
class ThemeViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<ThemeState> = settingsRepository.settings
        .map { ThemeState(style = it.uiStyle, darkMode = it.darkMode) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeState())
}
