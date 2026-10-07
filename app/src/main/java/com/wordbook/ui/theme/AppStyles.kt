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
    /**
     * 评分按钮圆角。
     * 999 = 全圆角药丸（Material 3 Expressive 的默认按钮形状）；
     * 0 = 方角（墨纸 / 马克笔这类硬朗风格）。
     */
    val ratingCorner: Int = 0,
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

// ------------------------------------------------------------------ 第二批风格（2026-10）
// 旧课本 / 极简白 / 拿铁 / 霓虹夜 / 铅字 / 蓝图 / 便签 / 森林 / 莫兰迪
//
// 浅色方案是按设计稿定的色值；深色变体由 tools/.cache/gen_schemes.py 按规则推导
// （底色压暗保持色相、强调色提亮保证对比度、on* 色相跟随对应角色），
// 生成后人工抽查过。深色专用的三套（霓虹夜/蓝图/森林）没有浅色版。

private val TextbookScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF23407A),
    onPrimary = Color(0xFFF7F1E1),
    primaryContainer = Color(0xFFDCE3F2),
    onPrimaryContainer = Color(0xFF16264A),
    secondary = Color(0xFF7A7057),
    onSecondary = Color(0xFFFFFCF2),
    secondaryContainer = Color(0xFFEAE3CE),
    onSecondaryContainer = Color(0xFF3A3423),
    tertiary = Color(0xFFC0392B),
    onTertiary = Color(0xFFFFF6F2),
    background = Color(0xFFF7F1E1),
    onBackground = Color(0xFF23304A),
    surface = Color(0xFFFFFCF2),
    onSurface = Color(0xFF23304A),
    surfaceVariant = Color(0xFFEFE7D2),
    surfaceContainer = Color(0xFFFFFCF2),
    surfaceContainerHigh = Color(0xFFEFE7D2),
    onSurfaceVariant = Color(0xFF7A7057),
    outline = Color(0xFFCFC5A9),
    outlineVariant = Color(0xFFE4DCC6),
    error = Color(0xFFA83228),
)

private val TextbookDarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF3968C7),
    onPrimary = Color(0xFF141924),
    primaryContainer = Color(0xFF575C66),
    onPrimaryContainer = Color(0xFFCAD5EB),
    secondary = Color(0xFFC7B78E),
    onSecondary = Color(0xFF141924),
    secondaryContainer = Color(0xFF5E5A4E),
    onSecondaryContainer = Color(0xFFCAD5EB),
    tertiary = Color(0xFFC73B2D),
    onTertiary = Color(0xFF141924),
    background = Color(0xFF13110C),
    onBackground = Color(0xFFEDE3C7),
    surface = Color(0xFF1B1811),
    onSurface = Color(0xFFEDE3C7),
    surfaceVariant = Color(0xFF221F15),
    surfaceContainer = Color(0xFF1B1811),
    surfaceContainerHigh = Color(0xFF2D281B),
    onSurfaceVariant = Color(0xFFA89E83),
    outline = Color(0xFF57503D),
    outlineVariant = Color(0xFF383427),
    error = Color(0xFFB8372C),
)

private val MinimalScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFFE02020),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFDE7E7),
    onPrimaryContainer = Color(0xFF7A1010),
    secondary = Color(0xFF555555),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEFEFEF),
    onSecondaryContainer = Color(0xFF222222),
    tertiary = Color(0xFF111111),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFF4F4F4),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF4F4F4),
    onSurfaceVariant = Color(0xFF777777),
    outline = Color(0xFFDDDDDD),
    outlineVariant = Color(0xFFEEEEEE),
    error = Color(0xFFD32F2F),
)

private val MinimalDarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFE02222),
    onPrimary = Color(0xFF241414),
    primaryContainer = Color(0xFF716161),
    onPrimaryContainer = Color(0xFFEBCACA),
    secondary = Color(0xFFC7C7C7),
    onSecondary = Color(0xFF241414),
    secondaryContainer = Color(0xFF635E5E),
    onSecondaryContainer = Color(0xFFEBCACA),
    tertiary = Color(0xFFC7C7C7),
    onTertiary = Color(0xFF241414),
    background = Color(0xFF130C0C),
    onBackground = Color(0xFFEDC7C7),
    surface = Color(0xFF1B1111),
    onSurface = Color(0xFFEDC7C7),
    surfaceVariant = Color(0xFF221515),
    surfaceContainer = Color(0xFF1B1111),
    surfaceContainerHigh = Color(0xFF2D1B1B),
    onSurfaceVariant = Color(0xFFA88383),
    outline = Color(0xFF573D3D),
    outlineVariant = Color(0xFF382727),
    error = Color(0xFFD32F2F),
)

private val LatteScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFFB4703A),
    onPrimary = Color(0xFFFFF9F2),
    primaryContainer = Color(0xFFF3E2D0),
    onPrimaryContainer = Color(0xFF4A2A12),
    secondary = Color(0xFF8A7561),
    onSecondary = Color(0xFFFFFBF5),
    secondaryContainer = Color(0xFFF0E3D2),
    onSecondaryContainer = Color(0xFF3A2E26),
    tertiary = Color(0xFFD99A4E),
    onTertiary = Color(0xFF3A2400),
    background = Color(0xFFFFFBF5),
    onBackground = Color(0xFF3A2E26),
    surface = Color(0xFFFFF6EA),
    onSurface = Color(0xFF3A2E26),
    surfaceVariant = Color(0xFFF6EADA),
    surfaceContainer = Color(0xFFFFF6EA),
    surfaceContainerHigh = Color(0xFFF6EADA),
    onSurfaceVariant = Color(0xFF8A7561),
    outline = Color(0xFFE0CEB8),
    outlineVariant = Color(0xFFF0E4D4),
    error = Color(0xFFC0503F),
)

private val LatteDarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFC77C40),
    onPrimary = Color(0xFF241B14),
    primaryContainer = Color(0xFF675D53),
    onPrimaryContainer = Color(0xFFEBD8CA),
    secondary = Color(0xFFC7A98C),
    onSecondary = Color(0xFF241B14),
    secondaryContainer = Color(0xFF645C52),
    onSecondaryContainer = Color(0xFFEBD8CA),
    tertiary = Color(0xFFD99A4E),
    onTertiary = Color(0xFF241B14),
    background = Color(0xFF13100C),
    onBackground = Color(0xFFEDDEC7),
    surface = Color(0xFF1B1711),
    onSurface = Color(0xFFEDDEC7),
    surfaceVariant = Color(0xFF221D15),
    surfaceContainer = Color(0xFF1B1711),
    surfaceContainerHigh = Color(0xFF2D251B),
    onSurfaceVariant = Color(0xFFA89983),
    outline = Color(0xFF574C3D),
    outlineVariant = Color(0xFF383127),
    error = Color(0xFFC0503F),
)

private val NewsprintScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF141414),
    onPrimary = Color(0xFFFBFAF7),
    primaryContainer = Color(0xFFE8E6E1),
    onPrimaryContainer = Color(0xFF141414),
    secondary = Color(0xFF6B6B67),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDEBE6),
    onSecondaryContainer = Color(0xFF262626),
    tertiary = Color(0xFFA02020),
    onTertiary = Color(0xFFFFF5F5),
    background = Color(0xFFFBFAF7),
    onBackground = Color(0xFF141414),
    surface = Color(0xFFFFFDF9),
    onSurface = Color(0xFF141414),
    surfaceVariant = Color(0xFFF0EEE9),
    surfaceContainer = Color(0xFFFFFDF9),
    surfaceContainerHigh = Color(0xFFF0EEE9),
    onSurfaceVariant = Color(0xFF6B6B67),
    outline = Color(0xFFD8D4CC),
    outlineVariant = Color(0xFFE8E5DF),
    error = Color(0xFFA02020),
)

private val NewsprintDarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFC7C7C7),
    onPrimary = Color(0xFF241414),
    primaryContainer = Color(0xFF5C5A54),
    onPrimaryContainer = Color(0xFFEBCACA),
    secondary = Color(0xFFC7C7BF),
    onSecondary = Color(0xFF241414),
    secondaryContainer = Color(0xFF615F59),
    onSecondaryContainer = Color(0xFFEBCACA),
    tertiary = Color(0xFFC72828),
    onTertiary = Color(0xFF241414),
    background = Color(0xFF13110C),
    onBackground = Color(0xFFEDE4C7),
    surface = Color(0xFF1B1811),
    onSurface = Color(0xFFEDE4C7),
    surfaceVariant = Color(0xFF221F15),
    surfaceContainer = Color(0xFF1B1811),
    surfaceContainerHigh = Color(0xFF2D281B),
    onSurfaceVariant = Color(0xFFA89F83),
    outline = Color(0xFF57503D),
    outlineVariant = Color(0xFF383427),
    error = Color(0xFFB82525),
)

private val StickyScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFFC4642F),
    onPrimary = Color(0xFFFFF6E8),
    primaryContainer = Color(0xFFFBE3C9),
    onPrimaryContainer = Color(0xFF5A2A10),
    secondary = Color(0xFFE8B04B),
    onSecondary = Color(0xFF3A2A00),
    secondaryContainer = Color(0xFFFCEFD2),
    onSecondaryContainer = Color(0xFF4A3B2A),
    tertiary = Color(0xFF6E9E6A),
    onTertiary = Color(0xFF0E2410),
    background = Color(0xFFFFFDF5),
    onBackground = Color(0xFF4A3B2A),
    surface = Color(0xFFFFF6C9),
    onSurface = Color(0xFF4A3B2A),
    surfaceVariant = Color(0xFFFFF0D6),
    surfaceContainer = Color(0xFFFFF6C9),
    surfaceContainerHigh = Color(0xFFFFF0D6),
    onSurfaceVariant = Color(0xFF9A8567),
    outline = Color(0xFFE8D5AE),
    outlineVariant = Color(0xFFF5E8CC),
    error = Color(0xFFC4553F),
)

private val StickyDarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFC76530),
    onPrimary = Color(0xFF241914),
    primaryContainer = Color(0xFF6F6253),
    onPrimaryContainer = Color(0xFFEBD5CA),
    secondary = Color(0xFFE8B04B),
    onSecondary = Color(0xFF241914),
    secondaryContainer = Color(0xFF706858),
    onSecondaryContainer = Color(0xFFEBD5CA),
    tertiary = Color(0xFF8AC785),
    onTertiary = Color(0xFF241914),
    background = Color(0xFF13120C),
    onBackground = Color(0xFFEDE6C7),
    surface = Color(0xFF1B1911),
    onSurface = Color(0xFFEDE6C7),
    surfaceVariant = Color(0xFF222015),
    surfaceContainer = Color(0xFF1B1911),
    surfaceContainerHigh = Color(0xFF2D291B),
    onSurfaceVariant = Color(0xFFA8A183),
    outline = Color(0xFF57513D),
    outlineVariant = Color(0xFF383527),
    error = Color(0xFFC4553F),
)

private val MorandiScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF7E8C90),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2E7E8),
    onPrimaryContainer = Color(0xFF333B3E),
    secondary = Color(0xFFC9AFA5),
    onSecondary = Color(0xFF3A2E29),
    secondaryContainer = Color(0xFFEFE4DF),
    onSecondaryContainer = Color(0xFF4A3A33),
    tertiary = Color(0xFFA8A88E),
    onTertiary = Color(0xFF2E2E22),
    background = Color(0xFFF7F5F1),
    onBackground = Color(0xFF3F4448),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF3F4448),
    surfaceVariant = Color(0xFFEFECE6),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEFECE6),
    onSurfaceVariant = Color(0xFF8A9095),
    outline = Color(0xFFD6D2CB),
    outlineVariant = Color(0xFFE8E4DE),
    error = Color(0xFFB5716A),
)

private val MorandiDarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFAEC1C7),
    onPrimary = Color(0xFF142024),
    primaryContainer = Color(0xFF555B5C),
    onPrimaryContainer = Color(0xFFCAE3EB),
    secondary = Color(0xFFC9AFA5),
    onSecondary = Color(0xFF142024),
    secondaryContainer = Color(0xFF635B57),
    onSecondaryContainer = Color(0xFFCAE3EB),
    tertiary = Color(0xFFC7C7A8),
    onTertiary = Color(0xFF142024),
    background = Color(0xFF13110C),
    onBackground = Color(0xFFEDE1C7),
    surface = Color(0xFF1B1711),
    onSurface = Color(0xFFEDE1C7),
    surfaceVariant = Color(0xFF221E15),
    surfaceContainer = Color(0xFF1B1711),
    surfaceContainerHigh = Color(0xFF2D271B),
    onSurfaceVariant = Color(0xFFA89C83),
    outline = Color(0xFF574E3D),
    outlineVariant = Color(0xFF383227),
    error = Color(0xFFB8736C),
)

private val NeonScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF22D3EE),
    onPrimary = Color(0xFF06243D),
    primaryContainer = Color(0xFF123A4A),
    onPrimaryContainer = Color(0xFFB8F1FB),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF1B1035),
    secondaryContainer = Color(0xFF241A44),
    onSecondaryContainer = Color(0xFFDDD3FF),
    tertiary = Color(0xFFFFD166),
    onTertiary = Color(0xFF2A1C00),
    background = Color(0xFF111729),
    onBackground = Color(0xFFE7ECFF),
    surface = Color(0xFF151D33),
    onSurface = Color(0xFFE7ECFF),
    surfaceVariant = Color(0xFF1B2440),
    surfaceContainer = Color(0xFF151D33),
    surfaceContainerHigh = Color(0xFF1B1F2D),
    onSurfaceVariant = Color(0xFF8A94B8),
    outline = Color(0xFF2A3A5E),
    outlineVariant = Color(0xFF1F2A47),
    error = Color(0xFFF0616D),
)

private val BlueprintScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF06243D),
    primaryContainer = Color(0xFF14385C),
    onPrimaryContainer = Color(0xFFCBE9FA),
    secondary = Color(0xFF8FB2D6),
    onSecondary = Color(0xFF0A1E33),
    secondaryContainer = Color(0xFF1B3A61),
    onSecondaryContainer = Color(0xFFD6E6F7),
    tertiary = Color(0xFFFFD166),
    onTertiary = Color(0xFF2A1C00),
    background = Color(0xFF0F2440),
    onBackground = Color(0xFFEAF3FF),
    surface = Color(0xFF12294A),
    onSurface = Color(0xFFEAF3FF),
    surfaceVariant = Color(0xFF16304F),
    surfaceContainer = Color(0xFF12294A),
    surfaceContainerHigh = Color(0xFF1B222D),
    onSurfaceVariant = Color(0xFF8FB2D6),
    outline = Color(0xFF2A4C78),
    outlineVariant = Color(0xFF1E3A5F),
    error = Color(0xFFF0616D),
)

private val ForestScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF7FB069),
    onPrimary = Color(0xFF10200F),
    primaryContainer = Color(0xFF28402A),
    onPrimaryContainer = Color(0xFFCFE8C4),
    secondary = Color(0xFF8FB295),
    onSecondary = Color(0xFF10200F),
    secondaryContainer = Color(0xFF243528),
    onSecondaryContainer = Color(0xFFD3E6D5),
    tertiary = Color(0xFFE0C17A),
    onTertiary = Color(0xFF2A2008),
    background = Color(0xFF16221A),
    onBackground = Color(0xFFE4EFE2),
    surface = Color(0xFF1C2A20),
    onSurface = Color(0xFFE4EFE2),
    surfaceVariant = Color(0xFF223326),
    surfaceContainer = Color(0xFF1C2A20),
    surfaceContainerHigh = Color(0xFF1B2D21),
    onSurfaceVariant = Color(0xFF8FB295),
    outline = Color(0xFF2E4634),
    outlineVariant = Color(0xFF243528),
    error = Color(0xFFD98A7C),
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

/** 中等圆角（极简白 / 莫兰迪 / 霓虹夜 / 蓝图） */
private val MediumShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

/** 大圆角（拿铁 / 便签 / 森林） */
private val RoundShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
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

/**
 * 衬线体（旧课本 / 铅字 / 便签）：标题和正文都走衬线，读起来像书。
 * 注：Android 没有可直接按名字取用的楷体，便签的"手写感"用衬线近似。
 */
private fun serifTypography(): Typography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif),
        headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
        bodyLarge = base.bodyLarge.copy(fontFamily = FontFamily.Serif),
        bodyMedium = base.bodyMedium.copy(fontFamily = FontFamily.Serif),
    )
}

/** 等宽体（蓝图）：音标、数字有工程图纸感 */
private fun monoTypography(): Typography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
        bodyMedium = base.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        labelLarge = base.labelLarge.copy(fontFamily = FontFamily.Monospace),
        labelSmall = base.labelSmall.copy(fontFamily = FontFamily.Monospace),
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
    // 保持原来的圆角按钮观感（这个令牌是后加的，默认 0 会变方角）
    ratingCorner = 16,
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

// ---------------------------------------------------------- 第二批风格的令牌
// 评分按钮全部 ratingCorner = 999（药丸），依据是 Material 3 Expressive 把全圆角
// 定为按钮默认形状，且药丸更好单手按中。

/** E 旧课本：方角细边 + 蓝墨水 + 衬线 */
private val TextbookTokens = AppStyleTokens(
    style = UiStyle.TEXTBOOK,
    cardCorner = 4,
    cardBorder = BorderStroke(1.dp, Color(0xFFCFC5A9)),
    cardElevation = 0,
    sectionTitle = SectionTitle.RULE,
    titleFont = FontFamily.Serif,
    wordFont = FontFamily.Serif,
    titleWeight = FontWeight.Bold,
    highlightBackground = Color(0xFFFCEFC2),
    highlightText = Color(0xFF23407A),
    ratingCorner = 999,
    rating = listOf(
        RatingButtonStyle(Color(0xFFC0392B), Color(0xFFFFF6F2)),
        RatingButtonStyle(Color(0xFFC8862F), Color(0xFFFFF9EE)),
        RatingButtonStyle(Color(0xFF2F6B57), Color(0xFFF0FBF6)),
        RatingButtonStyle(Color(0xFF23407A), Color(0xFFEEF3FF)),
    ),
)

/** F 极简白：无边框、靠细线划分、一抹正红 */
private val MinimalTokens = AppStyleTokens(
    style = UiStyle.MINIMAL,
    cardCorner = 8,
    cardBorder = null,
    cardElevation = 1,
    sectionTitle = SectionTitle.RULE,
    titleWeight = FontWeight.Bold,
    highlightBackground = null,
    highlightText = Color(0xFFE02020),
    ratingCorner = 999,
    rating = listOf(
        RatingButtonStyle(Color(0xFFE02020), Color(0xFFFFFFFF)),
        RatingButtonStyle(Color(0xFFFFFFFF), Color(0xFF111111), bordered = true, borderColor = Color(0xFF111111)),
        RatingButtonStyle(Color(0xFF111111), Color(0xFFFFFFFF)),
        RatingButtonStyle(Color(0xFFFFFFFF), Color(0xFF777777), bordered = true, borderColor = Color(0xFFCCCCCC)),
    ),
)

/** G 拿铁：奶油暖棕 + 大圆角 + 柔和阴影 */
private val LatteTokens = AppStyleTokens(
    style = UiStyle.LATTE,
    cardCorner = 24,
    cardBorder = null,
    cardElevation = 2,
    progressHeight = 10,
    highlightBackground = Color(0xFFFBE6C8),
    highlightText = Color(0xFF6B4423),
    titleWeight = FontWeight.Bold,
    ratingCorner = 999,
    rating = listOf(
        RatingButtonStyle(Color(0xFFD2705C), Color(0xFFFFFFFF)),
        RatingButtonStyle(Color(0xFFD99A4E), Color(0xFFFFFFFF)),
        RatingButtonStyle(Color(0xFF6E9E78), Color(0xFFFFFFFF)),
        RatingButtonStyle(Color(0xFF7E93B8), Color(0xFFFFFFFF)),
    ),
)

/** H 霓虹夜：深蓝紫 + 描边发光（不用填充） */
private val NeonTokens = AppStyleTokens(
    style = UiStyle.NEON,
    cardCorner = 14,
    cardBorder = BorderStroke(1.dp, Color(0xFF2A3A5E)),
    cardElevation = 0,
    progressHeight = 4,
    progressColor = Color(0xFF22D3EE),
    highlightBackground = Color(0x2622D3EE),
    highlightText = Color(0xFF22D3EE),
    sectionTitle = SectionTitle.WIDE,
    ratingCorner = 999,
    rating = listOf(
        RatingButtonStyle(Color(0xFF151D33), Color(0xFFF0616D), bordered = true, borderColor = Color(0xFFF0616D)),
        RatingButtonStyle(Color(0xFF151D33), Color(0xFFF0B45A), bordered = true, borderColor = Color(0xFFF0B45A)),
        RatingButtonStyle(Color(0xFF22D3EE), Color(0xFF06243D)),
        RatingButtonStyle(Color(0xFF151D33), Color(0xFFA78BFA), bordered = true, borderColor = Color(0xFFA78BFA)),
    ),
)

/** I 铅字：黑白 + 报头衬线 + 灰度分级 */
private val NewsprintTokens = AppStyleTokens(
    style = UiStyle.NEWSPRINT,
    cardCorner = 6,
    cardBorder = BorderStroke(1.dp, Color(0xFFD8D4CC)),
    cardElevation = 0,
    progressHeight = 4,
    progressRounded = false,
    progressColor = Color(0xFF141414),
    highlightBackground = null,
    highlightText = Color(0xFFA02020),
    sectionTitle = SectionTitle.RULE,
    titleFont = FontFamily.Serif,
    wordFont = FontFamily.Serif,
    titleWeight = FontWeight.Bold,
    ratingCorner = 999,
    rating = listOf(
        RatingButtonStyle(Color(0xFFA02020), Color(0xFFFFFFFF)),
        RatingButtonStyle(Color(0xFF6B6B67), Color(0xFFFFFFFF)),
        RatingButtonStyle(Color(0xFF141414), Color(0xFFFFFFFF)),
        RatingButtonStyle(Color(0xFFFBFAF7), Color(0xFF4A4A46), bordered = true, borderColor = Color(0xFFB8B6B0)),
    ),
)

/** J 蓝图：深蓝 + 虚线 + 等宽 */
private val BlueprintTokens = AppStyleTokens(
    style = UiStyle.BLUEPRINT,
    cardCorner = 6,
    cardBorder = BorderStroke(1.dp, Color(0xFF2A4C78)),
    cardElevation = 0,
    progressHeight = 6,
    progressRounded = false,
    progressBorder = BorderStroke(1.dp, Color(0xFF24507F)),
    progressColor = Color(0xFF4FC3F7),
    highlightBackground = Color(0x294FC3F7),
    highlightText = Color(0xFF9FD9F7),
    sectionTitle = SectionTitle.WIDE,
    ratingCorner = 999,
    rating = listOf(
        RatingButtonStyle(Color(0xFF12294A), Color(0xFFF0616D), bordered = true, borderColor = Color(0xFFF0616D)),
        RatingButtonStyle(Color(0xFF12294A), Color(0xFFFFD166), bordered = true, borderColor = Color(0xFFFFD166)),
        RatingButtonStyle(Color(0xFF4FC3F7), Color(0xFF06243D)),
        RatingButtonStyle(Color(0xFF12294A), Color(0xFF8FB2D6), bordered = true, borderColor = Color(0xFF8FB2D6)),
    ),
)

/** K 便签：便签纸 + 硬阴影（偏移实心块）+ 暖色 */
private val StickyTokens = AppStyleTokens(
    style = UiStyle.STICKY,
    cardCorner = 8,
    cardBorder = null,
    cardHardShadow = true,
    hardShadowColor = Color(0xFFE4D5A8),
    cardElevation = 0,
    progressHeight = 12,
    progressColor = Color(0xFFE8B04B),
    highlightBackground = Color(0xFFFFE9A8),
    highlightText = Color(0xFF8A5A2B),
    titleFont = FontFamily.Serif,
    titleWeight = FontWeight.Bold,
    ratingCorner = 999,
    rating = listOf(
        RatingButtonStyle(Color(0xFFE07A6B), Color(0xFFFFFFFF)),
        RatingButtonStyle(Color(0xFFE8B04B), Color(0xFFFFFFFF)),
        RatingButtonStyle(Color(0xFF7FAF7A), Color(0xFFFFFFFF)),
        RatingButtonStyle(Color(0xFF7FA6C9), Color(0xFFFFFFFF)),
    ),
)

/** L 森林：墨绿暗色 + 苔绿强调 */
private val ForestTokens = AppStyleTokens(
    style = UiStyle.FOREST,
    cardCorner = 16,
    cardBorder = BorderStroke(1.dp, Color(0xFF2E4634)),
    cardElevation = 1,
    progressHeight = 9,
    progressColor = Color(0xFF7FB069),
    highlightBackground = Color(0x2E7FB069),
    highlightText = Color(0xFFC8E6B4),
    titleWeight = FontWeight.Bold,
    ratingCorner = 999,
    rating = listOf(
        RatingButtonStyle(Color(0xFFB96A5E), Color(0xFFFFF3F0)),
        RatingButtonStyle(Color(0xFFC79A4E), Color(0xFF241A06)),
        RatingButtonStyle(Color(0xFF7FB069), Color(0xFF10200F)),
        RatingButtonStyle(Color(0xFF5E8CA8), Color(0xFFF0F7FB)),
    ),
)

/** M 莫兰迪：低饱和灰调 + 柔和阴影 */
private val MorandiTokens = AppStyleTokens(
    style = UiStyle.MORANDI,
    cardCorner = 18,
    cardBorder = null,
    cardElevation = 1,
    progressHeight = 8,
    progressColor = Color(0xFF8C9A9E),
    highlightBackground = Color(0xFFE4E9E1),
    highlightText = Color(0xFF4F5F4B),
    titleWeight = FontWeight.SemiBold,
    ratingCorner = 999,
    rating = listOf(
        RatingButtonStyle(Color(0xFFC08C86), Color(0xFFFFFFFF)),
        RatingButtonStyle(Color(0xFFC7A98A), Color(0xFFFFFFFF)),
        RatingButtonStyle(Color(0xFF93A891), Color(0xFFFFFFFF)),
        RatingButtonStyle(Color(0xFF93A3B5), Color(0xFFFFFFFF)),
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

    // ---------------------------------------------------------- 第二批
    UiStyle.TEXTBOOK -> StyleDefinition(
        scheme = if (dark) TextbookDarkScheme else TextbookScheme,
        shapes = SharpShapes,
        typography = serifTypography(),
        tokens = if (dark) TextbookTokens.copy(cardBorder = BorderStroke(1.dp, Color(0xFF3A342B))) else TextbookTokens,
    )
    UiStyle.MINIMAL -> StyleDefinition(
        scheme = if (dark) MinimalDarkScheme else MinimalScheme,
        shapes = MediumShapes,
        typography = baseTypography(),
        tokens = MinimalTokens,
    )
    UiStyle.LATTE -> StyleDefinition(
        scheme = if (dark) LatteDarkScheme else LatteScheme,
        shapes = RoundShapes,
        typography = baseTypography(),
        tokens = if (dark) LatteTokens.copy(hardShadowColor = Color(0xFF2A2118)) else LatteTokens,
    )
    // 霓虹夜 / 蓝图 / 森林本身就是深色风格，没有浅色版
    UiStyle.NEON -> StyleDefinition(NeonScheme, MediumShapes, baseTypography(), NeonTokens)
    UiStyle.NEWSPRINT -> StyleDefinition(
        scheme = if (dark) NewsprintDarkScheme else NewsprintScheme,
        shapes = SharpShapes,
        typography = serifTypography(),
        tokens = if (dark) NewsprintTokens.copy(cardBorder = BorderStroke(1.dp, Color(0xFF3A3833))) else NewsprintTokens,
    )
    UiStyle.BLUEPRINT -> StyleDefinition(BlueprintScheme, MediumShapes, monoTypography(), BlueprintTokens)
    UiStyle.STICKY -> StyleDefinition(
        scheme = if (dark) StickyDarkScheme else StickyScheme,
        shapes = RoundShapes,
        typography = serifTypography(),
        tokens = if (dark) StickyTokens.copy(hardShadowColor = Color(0xFF3A3122)) else StickyTokens,
    )
    UiStyle.FOREST -> StyleDefinition(ForestScheme, RoundShapes, baseTypography(), ForestTokens)
    UiStyle.MORANDI -> StyleDefinition(
        scheme = if (dark) MorandiDarkScheme else MorandiScheme,
        shapes = RoundShapes,
        typography = baseTypography(),
        tokens = MorandiTokens,
    )
}

val LocalAppStyle = staticCompositionLocalOf { SystemTokens }
