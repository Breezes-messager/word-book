package com.wordbook.domain.article

/**
 * 每日文章的规模规则（与需求文档一致）：
 *  - 今天学过的词少于 5 个不允许生成
 *  - 每篇最多 20 个目标词，超过就拆成多篇，绝不把词硬塞进一篇
 */
object ArticlePlan {

    /** 少于这个词数不允许生成 */
    const val MIN_WORDS = 5

    /** 单篇最多目标词 */
    const val MAX_PER_ARTICLE = 20

    /** 单篇最少目标词（不足时按实际数量生成） */
    const val MIN_PER_ARTICLE = 8

    /** 背景词汇表上限 */
    const val MAX_KNOWN_WORDS = 500

    /**
     * 把 total 个目标词切成若干篇，返回每篇的词数。
     * 5–20 个 → 一篇；21 个 → 11 + 10；41 个 → 14 + 14 + 13。
     */
    fun batchSizes(total: Int): List<Int> {
        if (total < MIN_WORDS) return emptyList()
        if (total <= MAX_PER_ARTICLE) return listOf(total)
        val batches = (total + MAX_PER_ARTICLE - 1) / MAX_PER_ARTICLE
        val base = total / batches
        var remainder = total % batches
        return (0 until batches).map {
            var size = base
            if (remainder > 0) {
                size += 1
                remainder -= 1
            }
            size
        }
    }
}
