package com.wordbook.domain.article

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 目标词在文章中的实际出现形态 */
@Serializable
data class ArticleOccurrence(
    val target: String,
    val surface: String,
    val paragraph: Int = 0,
)

/** DeepSeek 返回的文章结构 */
@Serializable
data class ArticleContent(
    val title: String = "",
    val titleCn: String = "",
    val paragraphs: List<String> = emptyList(),
    val occurrences: List<ArticleOccurrence> = emptyList(),
    val translation: List<String> = emptyList(),
)

/**
 * 文章 JSON 解析。模型偶尔会带上 Markdown 代码块或多余解释文字，
 * 这里统一剥离后再解析；失败时抛出带中文说明的异常，由上层提示用户。
 */
object ArticleJsonParser {

    /** Markdown 代码块围栏（三个反引号），用 \uXXXX 写成，避免源码里出现反引号 */
    private val fence = Regex("""\u0060\u0060\u0060(?:json)?""", RegexOption.IGNORE_CASE)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    /** 提取大括号之间的 JSON 主体 */
    fun extractJson(raw: String): String {
        var text = raw.trim()
        text = fence.replace(text, "")
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) {
            throw IllegalArgumentException("模型返回的内容里没有找到 JSON")
        }
        return text.substring(start, end + 1)
    }

    fun parse(raw: String): ArticleContent {
        val body = extractJson(raw)
        val content = try {
            json.decodeFromString(ArticleContent.serializer(), body)
        } catch (t: Throwable) {
            throw IllegalArgumentException("文章 JSON 解析失败：" + (t.message ?: t.javaClass.simpleName), t)
        }
        if (content.paragraphs.isEmpty()) {
            throw IllegalArgumentException("文章 JSON 里没有正文段落")
        }
        return content.copy(
            title = content.title.ifBlank { "Untitled" },
            paragraphs = content.paragraphs.map { it.trim() }.filter { it.isNotEmpty() },
            translation = content.translation.map { it.trim() },
        )
    }

    fun toJson(content: ArticleContent): String = json.encodeToString(ArticleContent.serializer(), content)

    fun fromJsonOrNull(raw: String): ArticleContent? = runCatching { parse(raw) }.getOrNull()
}
