package com.wordbook.domain.model

/** 搜索结果筛选标签 */
enum class SearchFilter(val label: String) {
    ALL("全部"),
    UNLEARNED("未学"),
    LEARNED("已学"),
    HARD("难词"),
}

/** 一行结果的匹配方式，决定标题里写「前缀匹配」还是「按释义匹配」 */
enum class MatchKind { PREFIX, MEANING }

/** 学习状态徽章 */
data class WordBadge(
    val label: String,
    /** 配色分组：new / learn / due / hard */
    val tone: String,
)

/** 一条搜索结果 */
data class SearchHit(
    /** 词条 id，点开/加入学习要用 */
    val wordId: Long,
    val headword: String,
    val phonetic: String?,
    /** 中文释义（可能多行） */
    val translation: String,
    val badge: WordBadge,
    val learned: Boolean,
    val hard: Boolean,
)

/** 搜索结果 + 筛选数量 */
data class SearchOutcome(
    val hits: List<SearchHit>,
    val counts: Map<SearchFilter, Int>,
    val matchKind: MatchKind,
    /** 命中的是词形还原后的原形（例如输 running 还原成 run） */
    val lemma: String? = null,
)
