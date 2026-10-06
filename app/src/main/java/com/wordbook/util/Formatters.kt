package com.wordbook.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** 把毫秒间隔格式化成 1 分钟 / 3 天 / 2 个月 这样的中文提示 */
fun formatInterval(millis: Long): String {
    if (millis <= 0) return "现在"
    val minutes = millis / 60000.0
    return when {
        minutes < 1 -> "1 分钟内"
        minutes < 60 -> minutes.roundToInt().toString() + " 分钟"
        minutes < 60 * 24 -> (minutes / 60).roundToInt().toString() + " 小时"
        minutes < 60 * 24 * 30 -> (minutes / 1440).roundToInt().toString() + " 天"
        minutes < 60 * 24 * 365 -> (minutes / 1440 / 30).roundToInt().toString() + " 个月"
        else -> (minutes / 1440 / 365).roundToInt().toString() + " 年"
    }
}

fun todayKey(): String = LocalDate.now().toString()

private val prettyFormatter = DateTimeFormatter.ofPattern("M 月 d 日")

fun prettyDate(dayKey: String): String = runCatching {
    LocalDate.parse(dayKey).format(prettyFormatter)
}.getOrDefault(dayKey)
