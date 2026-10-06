package com.wordbook.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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

@Composable
fun WordBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = WordBookTypography,
        content = content,
    )
}
