package com.wordbook.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordbook.domain.model.UiStyle

/** 区块标题的处理方式 */
enum class SectionTitle {
    /** 普通标题 */
    PLAIN,

    /** 宋体 + 底部细线（墨纸） */
    RULE,

    /** 大字距 + 细体（暗夜） */
    WIDE,

    /** 荧光底色 + 黑描边（马克笔） */
    BOX,
}

/** 四档评分按钮的样式 */
data class RatingButtonStyle(
    val background: Color,
    val content: Color,
    /** 是否描边（暗夜风用描边 + 彩色文字，不用填充） */
    val bordered: Boolean = false,
    val borderColor: Color = Color.Unspecified,
    /** 马克笔风的硬阴影 */
    val hardShadow: Boolean = false,
)

/**
 * 一套风格的全部"非 Material"令牌：卡片、进度条、评分按钮、高亮、字体族。
 * 组件通过 [LocalAppStyle] 读取，从而一套代码支持多种风格。
 */
data class AppStyleTokens(
    val style: UiStyle,
    /** 卡片圆角 */
    val cardCorner: Int,
    /** 卡片描边（null = 不描边） */
    val cardBorder: BorderStroke?,
    /** 硬阴影（偏移实心块） */
    val cardHardShadow: Boolean = false,
    /** 硬阴影颜色（浅色马克笔是黑，深色马克笔是白） */
    val hardShadowColor: Color = Color(0xFF101010),
    /** 卡片是否用 Material 的 elevation */
    val cardElevation: Int = 1,
    /** 进度条高度与是否圆角 */
    val progressHeight: Int = 8,
    val progressRounded: Boolean = true,
    /** 进度条描边（马克笔风用黑边） */
    val progressBorder: BorderStroke? = null,
    /** 进度条填充色（null = 用主题 primary） */
    val progressColor: Color? = null,
    /** 文章里目标词的高亮底色（null = 只改文字颜色 + 下划线） */
    val highlightBackground: Color? = null,
    /** 高亮文字颜色（null = 用主题 primary） */
    val highlightText: Color? = null,
    /** 区块标题的处理方式（各风格的辨识度主要靠它） */
    val sectionTitle: SectionTitle = SectionTitle.PLAIN,
    /** 大单词（词卡正面）用的字体 */
    val wordFont: FontFamily = FontFamily.SansSerif,
    /** 标题字体 */
    val titleFont: FontFamily = FontFamily.SansSerif,
    val titleWeight: FontWeight = FontWeight.SemiBold,
    val rating: List<RatingButtonStyle>,
)

// ------------------------------------------------------------------ 颜色方案

/** A · 墨纸：纸白 / 墨黑 / 朱红 */
private val InkScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFFB23A2E),
    onPrimary = Color(0xFFF7F4ED),
    primaryContainer = Color(0xFFF0DFD9),
    onPrimaryContainer = Color(0xFF5A1A12),
    secondary = Color(0xFF4A473D),
    onSecondary = Color(0xFFF7F4ED),
    secondaryContainer = Color(0xFFEDE8DC),
    onSecondaryContainer = Color(0xFF2A2820),
    tertiary = Color(0xFF35506F),
    onTertiary = Color(0xFFF7F4ED),
    background = Color(0xFFF7F4ED),
    onBackground = Color(0xFF1B1A17),
    surface = Color(0xFFFBF9F4),
    onSurface = Color(0xFF1B1A17),
    surfaceVariant = Color(0xFFF2EEE4),
    onSurfaceVariant = Color(0xFF7C755F),
    surfaceContainer = Color(0xFFFBF9F4),
    surfaceContainerHigh = Color(0xFFF2EEE4),
    outline = Color(0xFFD9D3C5),
    outlineVariant = Color(0xFFE4DFD3),
    error = Color(0xFFB23A2E),
)

/** B · 暗夜专注：近黑 / 薄荷荧光 */
private val MidnightScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF3DDC97),
    onPrimary = Color(0xFF06231A),
    primaryContainer = Color(0xFF12352A),
    onPrimaryContainer = Color(0xFFB9F5DC),
    secondary = Color(0xFF8892A0),
    onSecondary = Color(0xFF0B0D10),
    secondaryContainer = Color(0xFF1A1F26),
    onSecondaryContainer = Color(0xFFC7CFD9),
    tertiary = Color(0xFF4D9BFF),
    onTertiary = Color(0xFF04121F),
    background = Color(0xFF0B0D10),
    onBackground = Color(0xFFEEF1F5),
    surface = Color(0xFF0E1115),
    onSurface = Color(0xFFEEF1F5),
    surfaceVariant = Color(0xFF12161B),
    onSurfaceVariant = Color(0xFF8892A0),
    surfaceContainer = Color(0xFF12161B),
    surfaceContainerHigh = Color(0xFF171C22),
    outline = Color(0xFF2A3038),
    outlineVariant = Color(0xFF1E232B),
    error = Color(0xFFFF6B6B),
)

/** C · 薄荷圆润：奶油白 / 薄荷绿 */
private val MintScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF2F9E7E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDFF3EC),
    onPrimaryContainer = Color(0xFF10322A),
    secondary = Color(0xFF4E7167),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3EFE8),
    onSecondaryContainer = Color(0xFF17332B),
    tertiary = Color(0xFFE8A33D),
    onTertiary = Color(0xFF3A2400),
    background = Color(0xFFF3F8F4),
    onBackground = Color(0xFF10322A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF10322A),
    surfaceVariant = Color(0xFFDFF3EC),
    onSurfaceVariant = Color(0xFF6E8C81),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFE9F4EE),
    outline = Color(0xFFBFE2D4),
    outlineVariant = Color(0xFFE3EFE8),
    error = Color(0xFFE4695F),
)

/** D · 马克笔：纯白 / 纯黑 / 荧光黄 */
private val MarkerScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF101010),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE94A),
    onPrimaryContainer = Color(0xFF101010),
    secondary = Color(0xFF1F4FE0),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8E2FF),
    onSecondaryContainer = Color(0xFF00174A),
    tertiary = Color(0xFFE8A33D),
    onTertiary = Color(0xFF101010),
    background = Color(0xFFFFFDF2),
    onBackground = Color(0xFF101010),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101010),
    surfaceVariant = Color(0xFFFFF9DA),
    onSurfaceVariant = Color(0xFF4A4A4A),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFF9DA),
    outline = Color(0xFF101010),
    outlineVariant = Color(0xFFD5D5D5),
    error = Color(0xFFD93025),
)

/** A · 墨纸的暗色版（墨夜）：暖调近黑，朱红提亮 */
private val InkDarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFE0705F),
    onPrimary = Color(0xFF35100A),
    primaryContainer = Color(0xFF4A1A13),
    onPrimaryContainer = Color(0xFFF5D9D3),
    secondary = Color(0xFFC4BCA9),
    onSecondary = Color(0xFF2A2820),
    secondaryContainer = Color(0xFF2E2A22),
    onSecondaryContainer = Color(0xFFEDE7DA),
    tertiary = Color(0xFF8FB0D6),
    onTertiary = Color(0xFF0C1A2A),
    background = Color(0xFF14120F),
    onBackground = Color(0xFFEDE7DA),
    surface = Color(0xFF1A1714),
    onSurface = Color(0xFFEDE7DA),
    surfaceVariant = Color(0xFF221E19),
    onSurfaceVariant = Color(0xFFA69C88),
    surfaceContainer = Color(0xFF1A1714),
    surfaceContainerHigh = Color(0xFF221E19),
    outline = Color(0xFF3A342B),
    outlineVariant = Color(0xFF2A251E),
    error = Color(0xFFE0705F),
)

/** C · 薄荷的暗色版 */
private val MintDarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF4CC49A),
    onPrimary = Color(0xFF06231A),
    primaryContainer = Color(0xFF1E4636),
    onPrimaryContainer = Color(0xFFCDEEDF),
    secondary = Color(0xFF8FAFA2),
    onSecondary = Color(0xFF12271F),
    secondaryContainer = Color(0xFF24352D),
    onSecondaryContainer = Color(0xFFDCEAE2),
    tertiary = Color(0xFFE8A33D),
    onTertiary = Color(0xFF2A1C00),
    background = Color(0xFF0E1512),
    onBackground = Color(0xFFDCEAE2),
    surface = Color(0xFF141C18),
    onSurface = Color(0xFFDCEAE2),
    surfaceVariant = Color(0xFF1B2721),
    onSurfaceVariant = Color(0xFF8FAFA2),
    surfaceContainer = Color(0xFF141C18),
    surfaceContainerHigh = Color(0xFF1B2721),
    outline = Color(0xFF2A3B33),
    outlineVariant = Color(0xFF1E2B24),
    error = Color(0xFFE4695F),
)

/** D · 马克笔的暗色版（黑板 + 荧光笔） */
private val MarkerDarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFFFE94A),
    onPrimary = Color(0xFF16160F),
    primaryContainer = Color(0xFFFFE94A),
    onPrimaryContainer = Color(0xFF16160F),
    secondary = Color(0xFF9CC7FF),
    onSecondary = Color(0xFF00174A),
    secondaryContainer = Color(0xFF1B2A44),
    onSecondaryContainer = Color(0xFFD8E2FF),
    tertiary = Color(0xFFE8A33D),
    onTertiary = Color(0xFF16160F),
    background = Color(0xFF121212),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF26241A),
    onSurfaceVariant = Color(0xFFB8B8B8),
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF26241A),
    outline = Color(0xFFF2F2F2),
    outlineVariant = Color(0xFF3A3A3A),
    error = Color(0xFFFF8A80),
)

// ------------------------------------------------------------------ 圆角 / 字体

private val SharpShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

private val MarkerShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(3.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp),
)

private val SoftShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

fun baseTypography(): Typography = Typography()

/** A 墨纸：标题与单词用衬线，正文保持无衬线 */
private fun inkTypography(): Typography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif),
    )
}

/** B 暗夜：单词更大，标题字距更开 */
private fun midnightTypography(): Typography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontSize = 46.sp, lineHeight = 52.sp, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Light, letterSpacing = 2.sp),
    )
}

/** C 薄荷：标题更重 */
private fun mintTypography(): Typography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    )
}

/** D 马克笔：超粗标题 */
private fun markerTypography(): Typography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Black),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Black),
    )
}

// ------------------------------------------------------------------ 令牌

private val SystemTokens = AppStyleTokens(
    style = UiStyle.SYSTEM,
    cardCorner = 12,
    cardBorder = null,
    cardElevation = 1,
    progressHeight = 8,
    rating = listOf(
        RatingButtonStyle(Color(0xFFD1495B), Color.White),
        RatingButtonStyle(Color(0xFFE08A3C), Color.White),
        RatingButtonStyle(Color(0xFF2E8B6E), Color.White),
        RatingButtonStyle(Color(0xFF3A78C2), Color.White),
    ),
)

private val InkTokens = AppStyleTokens(
    style = UiStyle.INK,
    cardCorner = 0,
    cardBorder = BorderStroke(1.dp, Color(0xFFD9D3C5)),
    cardElevation = 0,
    progressHeight = 3,
    progressRounded = false,
    wordFont = FontFamily.Serif,
    titleFont = FontFamily.Serif,
    highlightText = Color(0xFFB23A2E),
    sectionTitle = SectionTitle.RULE,
    rating = listOf(
        RatingButtonStyle(Color(0xFFB23A2E), Color(0xFFF7F4ED)),
        RatingButtonStyle(Color(0xFFC8862F), Color(0xFFFBF9F4)),
        RatingButtonStyle(Color(0xFF2F6B57), Color(0xFFFBF9F4)),
        RatingButtonStyle(Color(0xFF35506F), Color(0xFFFBF9F4)),
    ),
)

private val MidnightTokens = AppStyleTokens(
    style = UiStyle.MIDNIGHT,
    cardCorner = 0,
    cardBorder = BorderStroke(1.dp, Color(0xFF222831)),
    cardElevation = 0,
    progressHeight = 3,
    progressRounded = false,
    highlightBackground = Color(0x263DDC97),
    highlightText = Color(0xFF3DDC97),
    sectionTitle = SectionTitle.WIDE,
    rating = listOf(
        RatingButtonStyle(Color.Transparent, Color(0xFFFF6B6B), bordered = true, borderColor = Color(0xFFFF6B6B)),
        RatingButtonStyle(Color.Transparent, Color(0xFFE8A33D), bordered = true, borderColor = Color(0xFFE8A33D)),
        RatingButtonStyle(Color(0x263DDC97), Color(0xFF3DDC97), bordered = true, borderColor = Color(0xFF3DDC97)),
        RatingButtonStyle(Color.Transparent, Color(0xFF4D9BFF), bordered = true, borderColor = Color(0xFF4D9BFF)),
    ),
)

private val MintTokens = AppStyleTokens(
    style = UiStyle.MINT,
    cardCorner = 20,
    cardBorder = null,
    cardElevation = 2,
    progressHeight = 8,
    highlightBackground = Color(0xFFDFF3EC),
    highlightText = Color(0xFF1E6B55),
    rating = listOf(
        RatingButtonStyle(Color(0xFFE4695F), Color.White),
        RatingButtonStyle(Color(0xFFE8A33D), Color.White),
        RatingButtonStyle(Color(0xFF2F9E7E), Color.White),
        RatingButtonStyle(Color(0xFF4A86C8), Color.White),
    ),
)

private val MarkerTokens = AppStyleTokens(
    style = UiStyle.MARKER,
    cardCorner = 4,
    cardBorder = BorderStroke(2.dp, Color(0xFF101010)),
    cardHardShadow = true,
    cardElevation = 0,
    progressHeight = 12,
    progressRounded = false,
    progressBorder = BorderStroke(2.dp, Color(0xFF101010)),
    progressColor = Color(0xFFFFD400),
    highlightBackground = Color(0xFFFFE94A),
    highlightText = Color(0xFF101010),
    titleWeight = FontWeight.Black,
    sectionTitle = SectionTitle.BOX,
    rating = listOf(
        RatingButtonStyle(Color(0xFFFF8A80), Color(0xFF101010), bordered = true, borderColor = Color(0xFF101010), hardShadow = true),
        RatingButtonStyle(Color(0xFFFFC46B), Color(0xFF101010), bordered = true, borderColor = Color(0xFF101010), hardShadow = true),
        RatingButtonStyle(Color(0xFF9BE8B5), Color(0xFF101010), bordered = true, borderColor = Color(0xFF101010), hardShadow = true),
        RatingButtonStyle(Color(0xFF9CC7FF), Color(0xFF101010), bordered = true, borderColor = Color(0xFF101010), hardShadow = true),
    ),
)

/** 暗色下的令牌微调：描边色要跟着底色换，否则看不见 */
private val InkTokensDark = InkTokens.copy(
    cardBorder = BorderStroke(1.dp, Color(0xFF3A342B)),
)

private val MarkerTokensDark = MarkerTokens.copy(
    cardBorder = BorderStroke(2.dp, Color(0xFFF2F2F2)),
    hardShadowColor = Color(0xFFF2F2F2),
    progressBorder = BorderStroke(2.dp, Color(0xFFF2F2F2)),
    highlightText = Color(0xFF16160F),
    rating = MarkerTokens.rating.map { it.copy(borderColor = Color(0xFFF2F2F2)) },
)

/**
 * 取某套风格在当前深浅模式下的完整定义。
 * @param dark 已经算好的"最终是否深色"（把用户设置、系统设置、风格自身的限制都合并了）
 * scheme = null 表示交给系统的动态取色（只有「跟随系统」这一档）
 */
data class StyleDefinition(
    val scheme: ColorScheme?,
    val shapes: Shapes,
    val typography: Typography,
    val tokens: AppStyleTokens,
)

fun definitionOf(style: UiStyle, dark: Boolean): StyleDefinition = when (style) {
    // 跟随系统：颜色交给 Material You / 系统深浅色
    UiStyle.SYSTEM -> StyleDefinition(null, Shapes(), baseTypography(), SystemTokens)
    UiStyle.INK -> StyleDefinition(
        scheme = if (dark) InkDarkScheme else InkScheme,
        shapes = SharpShapes,
        typography = inkTypography(),
        tokens = if (dark) InkTokensDark else InkTokens,
    )
    // 暗夜专注本身就是深色风格，不提供浅色版
    UiStyle.MIDNIGHT -> StyleDefinition(MidnightScheme, SharpShapes, midnightTypography(), MidnightTokens)
    UiStyle.MINT -> StyleDefinition(
        scheme = if (dark) MintDarkScheme else MintScheme,
        shapes = SoftShapes,
        typography = mintTypography(),
        tokens = MintTokens,
    )
    UiStyle.MARKER -> StyleDefinition(
        scheme = if (dark) MarkerDarkScheme else MarkerScheme,
        shapes = MarkerShapes,
        typography = markerTypography(),
        tokens = if (dark) MarkerTokensDark else MarkerTokens,
    )
}

val LocalAppStyle = staticCompositionLocalOf { SystemTokens }
