package com.wordbook.util

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * 极简 TTS 封装，只用来朗读单词（不朗读整篇文章）。
 * 初始化失败时静默降级，绝不崩溃。
 */
class Speaker(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        runCatching {
            tts = TextToSpeech(context.applicationContext) { status ->
                ready = status == TextToSpeech.SUCCESS
                if (ready) {
                    runCatching { tts?.language = Locale.US }
                }
            }
        }
    }

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        runCatching { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text) }
    }

    fun shutdown() {
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
    }
}

@Composable
fun rememberSpeaker(): Speaker {
    val context = LocalContext.current
    val speaker = remember { Speaker(context) }
    DisposableEffect(Unit) {
        onDispose { speaker.shutdown() }
    }
    return speaker
}
