package com.wordbook.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface DeepSeekApi {

    /**
     * DeepSeek Chat Completions，json_object 输出模式。
     * 注意：只发送单词与释义，不发送任何用户隐私数据。
     */
    @POST("chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest,
    ): ChatResponse
}
