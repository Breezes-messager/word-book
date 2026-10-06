package com.wordbook.domain.model

/** 文章风格，与设置页下拉框一一对应 */
enum class ArticleStyle(val label: String) {
    DIARY("日记"),
    STORY("小故事"),
    SCIENCE("科普短文"),
    DIALOGUE("对话");

    companion object {
        fun fromLabel(label: String): ArticleStyle = entries.firstOrNull { it.label == label } ?: STORY
    }
}

/** 全部设置项（DataStore 持久化） */
data class AppSettings(
    /** 每日新词数，默认 20，可改任意正整数 */
    val dailyNewWords: Int = 20,
    /** 每日复习上限，0 表示不限 */
    val dailyReviewLimit: Int = 0,
    /** 新词顺序：false=按词书顺序 true=乱序 */
    val shuffleNewWords: Boolean = false,
    val articleStyle: ArticleStyle = ArticleStyle.STORY,
    /** DeepSeek API Key，只存在本机 DataStore，不入库不进 git */
    val apiKey: String = "",
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    /** FSRS 目标记忆保持率 */
    val desiredRetention: Float = 0.9f,
    /** 文章页默认展开中文翻译 */
    val translationExpanded: Boolean = false,
    /** 界面风格（切换后立即生效） */
    val uiStyle: UiStyle = UiStyle.SYSTEM,
    /** 深浅色偏好 */
    val darkMode: DarkModeSetting = DarkModeSetting.FOLLOW_SYSTEM,
)
