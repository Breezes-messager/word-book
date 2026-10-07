package com.wordbook.domain.model

/**
 * 界面风格。四套设计语言 + 跟随系统，可在「设置 → 界面风格」里随时切换（立即生效）。
 *
 * 每套风格的颜色 / 圆角 / 字体 / 卡片样式定义见 ui/theme/AppStyles.kt。
 */
/** 深浅色偏好（「暗夜专注」风格固定深色，忽略此项） */
enum class DarkModeSetting(val label: String) {
    FOLLOW_SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色");

    companion object {
        fun fromName(name: String?): DarkModeSetting = entries.firstOrNull { it.name == name } ?: FOLLOW_SYSTEM
    }
}

enum class UiStyle(
    val label: String,
    val slogan: String,
    /** 风格预览用的小色块（背景 / 主色 / 点缀色） */
    val swatch: List<Long>,
    /** 本身就是深色风格，不跟随「深浅模式」设置 */
    val forceDark: Boolean = false,
) {
    SYSTEM(
        label = "跟随系统",
        slogan = "Material You 动态取色，支持深色模式",
        swatch = listOf(0xFFFBFDF9, 0xFF1F6F5C, 0xFF7D5700),
    ),
    INK(
        label = "墨纸",
        slogan = "米白纸感 · 宋体标题 · 朱红点睛",
        swatch = listOf(0xFFF7F4ED, 0xFFB23A2E, 0xFF1B1A17),
    ),
    MIDNIGHT(
        label = "暗夜专注",
        slogan = "近黑底 · 薄荷荧光 · 巨型单词",
        swatch = listOf(0xFF0B0D10, 0xFF3DDC97, 0xFF8892A0),
        forceDark = true,
    ),
    MINT(
        label = "薄荷圆润",
        slogan = "奶油白 · 大圆角 · 柔和阴影",
        swatch = listOf(0xFFF3F8F4, 0xFF2F9E7E, 0xFFDFF3EC),
    ),
    MARKER(
        label = "马克笔",
        slogan = "粗黑描边 · 荧光黄 · 硬阴影",
        swatch = listOf(0xFFFFFDF2, 0xFFFFE94A, 0xFF101010),
    ),

    // ---------------------------------------------------------- 第二批候选（2026-10）
    TEXTBOOK(
        label = "旧课本",
        slogan = "泛黄纸 · 蓝墨水 · 红批改 · 英语横格线",
        swatch = listOf(0xFFF7F1E1, 0xFF23407A, 0xFFC0392B),
    ),
    MINIMAL(
        label = "极简白",
        slogan = "纯白 · 发丝细线 · 一抹正红 · 大量留白",
        swatch = listOf(0xFFFFFFFF, 0xFFE02020, 0xFF111111),
    ),
    LATTE(
        label = "拿铁",
        slogan = "奶油暖棕 · 大圆角 · 柔和不刺眼",
        swatch = listOf(0xFFFFFBF5, 0xFFB4703A, 0xFFD99A4E),
    ),
    NEON(
        label = "霓虹夜",
        slogan = "深蓝紫 · 青紫霓虹 · 发光描边",
        swatch = listOf(0xFF111729, 0xFF22D3EE, 0xFFA78BFA),
        forceDark = true,
    ),
    NEWSPRINT(
        label = "铅字",
        slogan = "黑白 · 报纸报头 · 衬线大标题",
        swatch = listOf(0xFFFBFAF7, 0xFF141414, 0xFFA02020),
    ),
    BLUEPRINT(
        label = "蓝图",
        slogan = "深蓝底 · 虚线网格 · 等宽数字",
        swatch = listOf(0xFF0F2440, 0xFF4FC3F7, 0xFFFFD166),
        forceDark = true,
    ),
    STICKY(
        label = "便签",
        slogan = "奶油黄便签 · 楷体 · 纸片斜贴",
        swatch = listOf(0xFFFFF6C9, 0xFFC4642F, 0xFFE8B04B),
    ),
    FOREST(
        label = "森林",
        slogan = "墨绿底 · 苔绿强调 · 暖金点缀",
        swatch = listOf(0xFF16221A, 0xFF7FB069, 0xFFE0C17A),
        forceDark = true,
    ),
    MORANDI(
        label = "莫兰迪",
        slogan = "低饱和灰调 · 雾霾蓝灰 + 灰粉 + 燕麦",
        swatch = listOf(0xFFF7F5F1, 0xFF7E8C90, 0xFFC9AFA5),
    );

    companion object {
        fun fromName(name: String?): UiStyle = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
