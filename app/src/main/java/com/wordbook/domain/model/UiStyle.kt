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
    );

    companion object {
        fun fromName(name: String?): UiStyle = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
