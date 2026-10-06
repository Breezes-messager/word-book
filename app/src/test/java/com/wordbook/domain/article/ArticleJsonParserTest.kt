package com.wordbook.domain.article

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ArticleJsonParserTest {

    private val validJson = """
        {
          "title": "A Running Start",
          "titleCn": "从一个清晨开始",
          "paragraphs": ["I went running today.", "It was great."],
          "occurrences": [{"target": "run", "surface": "running", "paragraph": 0}],
          "translation": ["我今天去跑步了。", "感觉很好。"]
        }
    """.trimIndent()

    @Test
    fun `解析标准 JSON`() {
        val content = ArticleJsonParser.parse(validJson)
        assertEquals("A Running Start", content.title)
        assertEquals(2, content.paragraphs.size)
        assertEquals(1, content.occurrences.size)
        assertEquals("running", content.occurrences.first().surface)
        assertEquals(2, content.translation.size)
    }

    @Test
    fun `剥掉 Markdown 代码块`() {
        val fenced = "\u0060\u0060\u0060json\n" + validJson + "\n\u0060\u0060\u0060"
        val content = ArticleJsonParser.parse(fenced)
        assertEquals("A Running Start", content.title)
    }

    @Test
    fun `忽略 JSON 前后的解释文字`() {
        val messy = "好的，下面是文章：\n" + validJson + "\n希望你喜欢！"
        assertEquals("A Running Start", ArticleJsonParser.parse(messy).title)
    }

    @Test
    fun `未知字段不影响解析`() {
        val extra = validJson.trimEnd().removeSuffix("}") + ", \"extra\": 42}"
        assertEquals("A Running Start", ArticleJsonParser.parse(extra).title)
    }

    @Test
    fun `没有正文段落时报中文错误`() {
        val bad = "{\"title\": \"x\", \"paragraphs\": []}"
        try {
            ArticleJsonParser.parse(bad)
            fail("应当抛出异常")
        } catch (t: IllegalArgumentException) {
            assertTrue(t.message!!.contains("正文段落"))
        }
    }

    @Test
    fun `完全不是 JSON 时报错`() {
        try {
            ArticleJsonParser.parse("抱歉，我无法生成。")
            fail("应当抛出异常")
        } catch (t: IllegalArgumentException) {
            assertTrue(t.message!!.contains("没有找到 JSON"))
        }
    }

    @Test
    fun `序列化后能再解析回来`() {
        val content = ArticleJsonParser.parse(validJson)
        val roundTrip = ArticleJsonParser.parse(ArticleJsonParser.toJson(content))
        assertEquals(content, roundTrip)
    }
}
