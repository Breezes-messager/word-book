package com.wordbook.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.wordbook.domain.model.DarkModeSetting
import com.wordbook.domain.model.UiStyle

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = GreenOnPrimaryContainer,
    secondary = TealSecondary,
    onSecondary = TealOnSecondary,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = TealOnSecondaryContainer,
    tertiary = SandTertiary,
    onTertiary = SandOnTertiary,
    tertiaryContainer = SandTertiaryContainer,
    onTertiaryContainer = SandOnTertiaryContainer,
    background = SurfaceLight,
    surface = SurfaceLight,
    error = ErrorRed,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD8BC),
    onPrimary = Color(0xFF003829),
    primaryContainer = Color(0xFF005140),
    onPrimaryContainer = GreenPrimaryContainer,
    secondary = Color(0xFFB1CCC2),
    onSecondary = Color(0xFF1D352D),
    secondaryContainer = Color(0xFF334B44),
    onSecondaryContainer = TealSecondaryContainer,
    tertiary = Color(0xFFF0C070),
    onTertiary = Color(0xFF422C00),
    tertiaryContainer = Color(0xFF5F4100),
    onTertiaryContainer = SandTertiaryContainer,
    background = SurfaceDark,
    surface = SurfaceDark,
    error = Color(0xFFFFB4AB),
)

/**
 * 主题入口。风格由「设置 → 界面风格」决定，切换后立即生效：
 *  - SYSTEM：Material You 动态取色，跟随系统深浅色
 *  - 其余各套：固定外观（颜色 / 圆角 / 字体 / 卡片样式都不同），定义见 ui/theme/AppStyles.kt
 *
 * 组件通过 [LocalAppStyle] 读取风格令牌（卡片描边、进度条、评分按钮、高亮等）。
 */
@Composable
fun WordBookTheme(
    style: UiStyle = UiStyle.SYSTEM,
    darkMode: DarkModeSetting = DarkModeSetting.FOLLOW_SYSTEM,
    systemDark: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // 最终深浅：风格的硬性限制 > 用户设置 > 系统
    val effectiveDark = when {
        // 暗夜专注 / 霓虹夜 / 蓝图 / 森林 本身就是深色风格，忽略深浅模式设置
        style.forceDark -> true
        darkMode == DarkModeSetting.DARK -> true
        darkMode == DarkModeSetting.LIGHT -> false
        else -> systemDark
    }
    val definition = definitionOf(style, effectiveDark)

    val colorScheme = definition.scheme ?: when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (effectiveDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        effectiveDark -> DarkColors
        else -> LightColors
    }

    // 深色外观下状态栏 / 导航栏图标要转成浅色，否则看不见
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            runCatching {
                val window = (view.context as Activity).window
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !effectiveDark
                controller.isAppearanceLightNavigationBars = !effectiveDark
            }
        }
    }

    CompositionLocalProvider(LocalAppStyle provides definition.tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = definition.shapes,
            typography = definition.typography,
            content = content,
        )
    }
}
