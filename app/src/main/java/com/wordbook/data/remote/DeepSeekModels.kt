package com.wordbook.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
data class ResponseFormat(val type: String = "json_object")

@Serializable
data class ChatRequest(
    val model: String = "deepseek-chat",
    val messages: List<ChatMessage>,
    @SerialName("response_format") val responseFormat: ResponseFormat? = ResponseFormat(),
    val temperature: Double = 1.0,
    val stream: Boolean = false,
)

@Serializable
data class ChatChoice(
    val index: Int = 0,
    val message: ChatMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class ChatUsage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0,
)

@Serializable
data class ChatResponse(
    val id: String = "",
    val model: String = "",
    val choices: List<ChatChoice> = emptyList(),
    val usage: ChatUsage? = null,
)

/** 网络层统一异常，带上可以直接给用户看的中文提示 */
class DeepSeekException(val userMessage: String, val raw: String? = null, cause: Throwable? = null) :
    Exception(userMessage, cause)
