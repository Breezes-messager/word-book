package com.wordbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordbook.ui.WordBookNavHost
import com.wordbook.ui.theme.ThemeViewModel
import com.wordbook.ui.theme.WordBookTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 界面风格从设置里读，改完立即生效（不用重启）
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val theme by themeViewModel.state.collectAsStateWithLifecycle()
            WordBookTheme(style = theme.style, darkMode = theme.darkMode) {
                WordBookNavHost()
            }
        }
    }
}
