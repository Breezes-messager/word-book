package com.wordbook.data.remote

import android.util.Log
import com.wordbook.domain.article.ArticleContent
import com.wordbook.domain.article.ArticleJsonParser
import com.wordbook.domain.model.ArticleStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 调用 DeepSeek 生成每日文章。
 *
 * 提示词模板严格按需求实现（目标词 + 背景词汇表 + 风格 + JSON 输出）。
 * 失败会重试一次；仍然失败则抛出带中文说明的 [DeepSeekException]。
 */
@Singleton
class ArticleGenerator @Inject constructor(
    private val api: DeepSeekApi,
    private val json: Json,
) {
    companion object {
        private const val TAG = "ArticleGenerator"
        private const val MODEL = "deepseek-chat"
        private const val MAX_ATTEMPTS = 2
    }

    suspend fun generate(
        apiKey: String,
        targetWords: List<String>,
        knownWords: List<String>,
        style: ArticleStyle,
    ): ArticleContent = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw DeepSeekException("还没有填写 DeepSeek API Key，请到「设置」里填写")
        }

        var lastError: DeepSeekException? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val response = api.chatCompletions(
                    authorization = "Bearer " + apiKey.trim(),
                    request = ChatRequest(
                        model = MODEL,
                        messages = listOf(
                            ChatMessage(role = "user", content = buildPrompt(targetWords, knownWords, style)),
                        ),
                        responseFormat = ResponseFormat("json_object"),
                    ),
                )
                val raw = response.choices.firstOrNull()?.message?.content
                    ?: throw DeepSeekException("模型没有返回内容（choices 为空）")
                return@withContext try {
                    ArticleJsonParser.parse(raw)
                } catch (t: IllegalArgumentException) {
                    throw DeepSeekException("模型返回的文章格式不对：" + (t.message ?: ""), raw, t)
                }
            } catch (t: Throwable) {
                lastError = toDeepSeekException(t)
                Log.w(TAG, "第 " + (attempt + 1) + " 次生成失败：" + lastError?.userMessage, t)
            }
        }
        throw lastError ?: DeepSeekException("生成失败，请稍后重试")
    }

    /** 测试连接：用一个极小的请求验证 API Key 是否可用 */
    suspend fun testConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.chatCompletions(
                authorization = "Bearer " + apiKey.trim(),
                request = ChatRequest(
                    model = MODEL,
                    messages = listOf(ChatMessage(role = "user", content = "Reply with the single word: ok")),
                    responseFormat = null,
                    temperature = 0.0,
                ),
            )
            response.choices.firstOrNull()?.message?.content?.trim()?.take(40) ?: "连接成功"
        }.recoverCatching { throw toDeepSeekException(it) }
    }

    private fun toDeepSeekException(t: Throwable): DeepSeekException = when (t) {
        is DeepSeekException -> t
        is UnknownHostException -> DeepSeekException("网络不可用，检查一下网络连接（离线时仍可学习、复习、查词）", cause = t)
        is SocketTimeoutException -> DeepSeekException("请求超时，稍后再试", cause = t)
        is IOException -> DeepSeekException("网络请求失败：" + (t.message ?: "未知网络错误"), cause = t)
        is HttpException -> {
            val body = runCatching { t.response()?.errorBody()?.string() }.getOrNull()
            val hint = when (t.code()) {
                401 -> "API Key 无效，请检查设置里的 Key"
                402 -> "账户余额不足，请到 DeepSeek 官网充值"
                429 -> "请求太频繁，稍等一会儿再试"
                in 500..599 -> "DeepSeek 服务端返回错误（" + t.code() + "），稍后再试"
                else -> "请求被拒绝（HTTP " + t.code() + "）"
            }
            DeepSeekException(hint, body, t)
        }
        else -> DeepSeekException("生成失败：" + (t.message ?: t.javaClass.simpleName), cause = t)
    }

    /** 按需求文档给出的提示词模板拼装 */
    fun buildPrompt(targetWords: List<String>, knownWords: List<String>, style: ArticleStyle): String {
        val target = targetWords.joinToString("、")
        val known = knownWords.joinToString(", ")
        return listOf(
            "你是一位英语教学文章作者。请为一位中国考研学生写一篇英文短文。",
            "",
            "【必须自然使用的目标词】(每个至少出现一次)",
            target,
            "",
            "【学生已掌握的背景词汇】(尽量只用这些词和它们之外的常见基础词)",
            known,
            "",
            "要求：",
            "1. 只使用目标词、背景词汇表里的词，以及最常用的 2000 个英语基础词；严禁使用明显超纲的生僻词。",
            "2. 目标词要自然地融入上下文，不要生硬堆砌，不要写成词语接龙。",
            "3. 篇幅 3–5 段，每段 2–4 句，难度适合考研阅读水平。",
            "4. 风格：" + style.label + "（可选：日记 / 小故事 / 科普短文 / 对话）",
            "5. 目标词可以按语法需要使用时态或复数变化，不必强用原形。",
            "6. 只输出 JSON，不要输出任何解释文字。",
            "",
            "输出 JSON 结构：",
            "{",
            "  \"title\": \"英文标题\",",
            "  \"titleCn\": \"标题中文翻译\",",
            "  \"paragraphs\": [\"第一段\", \"第二段\", \"...\"],",
            "  \"occurrences\": [",
            "    {\"target\": \"run\", \"surface\": \"running\", \"paragraph\": 0}",
            "  ],",
            "  \"translation\": [\"第一段中文翻译\", \"...\"]",
            "}",
            "occurrences 里必须列出所有目标词在文中出现的实际形态，用于客户端高亮和点击查词。",
        ).joinToString("\n")
    }

    /** 便于调试：把 JSON 序列化回来 */
    fun encode(content: ArticleContent): String = json.encodeToString(ArticleContent.serializer(), content)
}
