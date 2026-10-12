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

    /**
     * 拼装给模型的提示词。
     *
     * 考研真题档（[ArticleStyle.examLevel]）会把难度显式顶上去：380–450 词、至少 4 个长难句、
     * 允许少量超纲词、作者态度隐含。
     *
     * 旧模板写的是「严禁使用明显超纲的生僻词」+「3–5 段、每段 2–4 句」，
     * 结果每篇只有 150–250 词、几乎全是简单句 —— 这就是「文章难度偏低」的根源。
     */
    fun buildPrompt(targetWords: List<String>, knownWords: List<String>, style: ArticleStyle): String {
        val target = targetWords.joinToString("、")
        val known = knownWords.joinToString(", ")

        val rules = mutableListOf<String>()
        rules += "目标词要自然地融入上下文，不要生硬堆砌，也不要写成词语接龙；" +
            "可以按语法需要改变时态、单复数或词形，不必强用原形。"
        rules += "风格：" + style.label + "。" + style.brief
        if (style.examLevel) {
            rules += "词汇难度对标考研英语（一）真题：以考研大纲词汇为主体，" +
                "允许少量（不超过 5%）超纲词 —— 真题里本来就有，不必刻意回避，" +
                "但也不要为了难而难、堆砌生僻术语。"
            rules += "篇幅 380–450 词，4–6 段。结构参考真题：" +
                "开篇引入话题并亮明观点 → 中间分论点配例证、数据或研究 → 结尾收束观点。"
            rules += "至少写 4 个长难句，包含从句、插入语、分词结构或倒装 —— 长难句正是考研阅读的主要难点。"
            rules += "语气书面、客观，可以含蓄地表达作者态度（surprisingly / unfortunately / arguably 这类词），" +
                "不必直白点破。"
        } else {
            rules += "词汇主要用学生已掌握的词和最常用的 2000 个英语基础词，不要用生僻词。"
            rules += "句子长短交错，读起来自然顺畅。"
        }
        rules += "只输出 JSON，不要输出任何解释文字。"

        val head = listOf(
            "你是一位英语教学文章作者，为一位正在准备中国考研英语的学生写英文短文。",
            "",
            "【必须自然使用的目标词】(每个至少出现一次)",
            target,
            "",
            "【学生已掌握的背景词汇】",
            known,
            "",
            "要求：",
        )
        val body = rules.mapIndexed { index, rule -> (index + 1).toString() + ". " + rule }
        val tail = listOf(
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
        )
        return (head + body + tail).joinToString("\n")
    }

    /** 便于调试：把 JSON 序列化回来 */
    fun encode(content: ArticleContent): String = json.encodeToString(ArticleContent.serializer(), content)
}
