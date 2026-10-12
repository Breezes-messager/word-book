package com.wordbook.domain.model

/**
 * 文章风格。
 *
 * 分两档：
 *  - **轻松档**（日记 / 小故事 / 科普短文 / 对话）—— 短、好读，用来保持阅读手感；
 *  - **考研真题档**（考研·经济 / 社科 / 科技 / 文化教育）—— 对标考研英语（一）阅读真题：
 *    380–450 词、长难句密集、作者态度隐含。
 *
 * 真题档的题材与题源按近 5 年真题分布来：社科 35% / 经济 25% / 科技 25% / 文化教育 15%，
 * 题源以《经济学人》《卫报》《科学美国人》《大西洋月刊》为主，几乎不考纯文学艺术。
 *
 * [brief] 会原样拼进给模型的提示词，所以写得越具体，生成的文章越像想要的样子。
 */
enum class ArticleStyle(val label: String, val brief: String, val examLevel: Boolean) {
    DIARY(
        label = "日记",
        brief = "第一人称日记体，写今天发生的一件小事和感受，语气轻松自然。150–220 词，3–4 段。",
        examLevel = false,
    ),
    STORY(
        label = "小故事",
        brief = "叙事短文，讲一个完整的小故事，有起承转合。200–280 词，3–5 段。",
        examLevel = false,
    ),
    SCIENCE(
        label = "科普短文",
        brief = "科普说明文，把一个小知识点讲清楚，语气平实。200–300 词，3–5 段。",
        examLevel = false,
    ),
    DIALOGUE(
        label = "对话",
        brief = "两人或多人的对话，围绕一个话题展开，用 A: / B: 标记说话人。150–250 词。",
        examLevel = false,
    ),
    EXAM_ECON(
        label = "考研·经济",
        brief = "对标《经济学人》社论：全球经济、产业结构、就业市场、公共政策或监管话题，" +
            "观点鲜明、论证层层推进，长难句密度高。",
        examLevel = true,
    ),
    EXAM_SOCIETY(
        label = "考研·社科",
        brief = "对标《卫报》《大西洋月刊》评论：社会现象、公共政策、教育、心理健康或城市生活，" +
            "叙事与议论结合，作者态度隐含在措辞里。",
        examLevel = true,
    ),
    EXAM_TECH(
        label = "考研·科技",
        brief = "对标《科学美国人》：人工智能、生物医学、气候环境或心理学研究，" +
            "结构为「现象 → 研究 → 结论」，并点出研究的局限或争议。",
        examLevel = true,
    ),
    EXAM_CULTURE(
        label = "考研·文化教育",
        brief = "对标《大西洋月刊》文化评论：教育体系、代际差异、文化现象或工作方式变迁，" +
            "先摆现象再分析利弊，最后收束观点。",
        examLevel = true,
    );

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
    /** 文章正文字号倍率（0.85–1.6） */
    val articleFontScale: Float = 1.0f,
    /** 文章行距倍率（1.0–1.9） */
    val articleLineHeightScale: Float = 1.0f,
    /**
     * 上次看的文章，格式 "日期:批次"，例如 "2026-10-07:3"。
     * 切到别的 Tab 再回来时，文章页的 ViewModel 会重建，靠它恢复上次看的那一篇。
     */
    val lastArticleSelection: String = "",
    /** 搜索历史，换行分隔，最近的在最前 */
    val searchHistory: String = "",
) {
    fun historyList(): List<String> = searchHistory.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
}
