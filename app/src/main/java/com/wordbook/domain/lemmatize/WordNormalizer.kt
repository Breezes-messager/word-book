package com.wordbook.domain.lemmatize

/**
 * 文章里点到的单词要先“洗干净”，再拿去查词典。
 * 需要处理：大小写、句尾标点、引号、连字符、所有格 's、常见缩写。
 *
 * 这里刻意不依赖 Android，方便单元测试。
 */
object WordNormalizer {

    private val leadingJunk = Regex("" + "^[^A-Za-z]+")
    private val trailingJunk = Regex("" + "[^A-Za-z'’-]+" + "$")
    private val possessive = Regex("" + "['’]s" + "$")

    /** 常见缩写后缀（去掉后通常能查到词根） */
    private val contractions = listOf("n't", "'re", "'ve", "'ll", "'d", "'m", "'s")

    /** 基础清洗：去标点、统一小写、保留词内连字符与撇号 */
    fun normalize(raw: String): String {
        var word = raw.trim()
        word = word.replace("’", "'")
        // 注意：只去掉开头的非字母字符，用 replace 会把词中间的撇号、连字符也一起吃掉
        word = leadingJunk.replaceFirst(word, "")
        word = trailingJunk.replace(word, "")
        return word.lowercase()
    }

    /**
     * 按“从精确到宽松”的顺序给出候选词形，查词时依次尝试。
     * 例如 Mom's, → [mom's, mom]；don't → [don't, do]；well-known → [well-known, well known, well]
     */
    fun candidates(raw: String): List<String> {
        val base = normalize(raw)
        if (base.isEmpty()) return emptyList()
        val result = LinkedHashSet<String>()
        result += base

        // 所有格
        val noPossessive = possessive.replace(base, "")
        if (noPossessive.isNotEmpty() && noPossessive != base) result += noPossessive

        // 缩写
        contractions.forEach { suffix ->
            if (base.endsWith(suffix) && base.length > suffix.length) {
                val stripped = base.dropLast(suffix.length)
                if (stripped.isNotEmpty()) result += stripped
            }
        }

        // 连字符：整词查不到时，试试拆开的写法与第一段
        if (base.contains("-")) {
            result += base.replace('-', ' ')
            base.split("-").firstOrNull { it.isNotEmpty() }?.let { result += it }
        }

        return result.filter { it.isNotEmpty() }
    }

    /** 判断一个 token 是不是可以点的“单词” */
    fun isLookupable(token: String): Boolean {
        val cleaned = normalize(token)
        return cleaned.isNotEmpty() && cleaned.any { it.isLetter() }
    }
}
