package com.wordbook.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordbook.data.prefs.SettingsRepository
import com.wordbook.domain.model.UiStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** 只负责把「界面风格」设置暴露给主题层，切换后立即重组生效 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val style: StateFlow<UiStyle> = settingsRepository.settings
        .map { it.uiStyle }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiStyle.SYSTEM)
}
