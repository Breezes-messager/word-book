package com.wordbook.data

import com.wordbook.data.remote.ArticleGenerator
import com.wordbook.data.remote.ChatRequest
import com.wordbook.data.remote.ChatResponse
import com.wordbook.data.remote.DeepSeekApi
import com.wordbook.domain.model.ArticleStyle
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 提示词的回归测试。
 *
 * 起因：文章难度一直偏低。查下来是提示词里写着「严禁使用明显超纲的生僻词」+
 * 「3–5 段，每段 2–4 句」，等于要求模型写 150–250 词的简单短文。
 * 这里把「考研档必须够长、必须有长难句」钉住，避免以后又被改回去。
 */
class ArticlePromptTest {

    /** 只用来拼提示词，不会真的发请求，所以 API 实现直接抛异常 */
    private val generator = ArticleGenerator(
        api = object : DeepSeekApi {
            override suspend fun chatCompletions(
                authorization: String,
                request: ChatRequest,
            ): ChatResponse = throw UnsupportedOperationException("这个测试不调用网络")
        },
        json = Json { ignoreUnknownKeys = true },
    )

    private fun prompt(style: ArticleStyle) =
        generator.buildPrompt(listOf("energy", "influence"), listOf("the", "of", "a"), style)

    @Test
    fun 考研档要求长篇与长难句() {
        ArticleStyle.entries.filter { it.examLevel }.forEach { style ->
            val text = prompt(style)
            assertTrue(style.label + " 应该写明 380–450 词", text.contains("380–450 词"))
            assertTrue(style.label + " 应该要求长难句", text.contains("长难句"))
            assertTrue(style.label + " 应该允许少量超纲词", text.contains("超纲词"))
            assertTrue(style.label + " 的风格说明应该进提示词", text.contains(style.brief))
        }
    }

    @Test
    fun 轻松档不会要求长篇() {
        ArticleStyle.entries.filter { !it.examLevel }.forEach { style ->
            val text = prompt(style)
            assertTrue(style.label + " 不该出现 380–450 词", !text.contains("380–450 词"))
            assertTrue(style.label + " 的风格说明应该进提示词", text.contains(style.brief))
        }
    }

    @Test
    fun 旧的强制简单词要求已经删掉() {
        ArticleStyle.entries.forEach { style ->
            assertTrue(
                style.label + " 不该再有「严禁使用明显超纲的生僻词」",
                !prompt(style).contains("严禁使用明显超纲的生僻词"),
            )
        }
    }

    @Test
    fun 目标词与输出格式仍在提示词里() {
        val text = prompt(ArticleStyle.EXAM_ECON)
        assertTrue(text.contains("energy"))
        assertTrue(text.contains("influence"))
        assertTrue(text.contains("occurrences"))
        assertTrue(text.contains("translation"))
    }

    @Test
    fun 风格标签唯一() {
        val labels = ArticleStyle.entries.map { it.label }
        assertEquals("风格标签不能重复，否则按 label 反查会错", labels.size, labels.toSet().size)
    }
}
