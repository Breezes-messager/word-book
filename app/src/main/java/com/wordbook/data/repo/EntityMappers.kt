package com.wordbook.data.repo

import com.wordbook.data.db.CardEntity
import com.wordbook.data.db.WordEntity
import com.wordbook.domain.fsrs.CardState
import com.wordbook.domain.fsrs.FsrsCard
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

/** 例句 / 短语在库里以 JSON 数组保存 */
@Serializable
data class SentencePair(val en: String = "", val cn: String = "")

private val json = Json { ignoreUnknownKeys = true }

fun WordEntity.examples(): List<SentencePair> = parsePairs(examplesJson)
fun WordEntity.phrases(): List<SentencePair> = parsePairs(phrasesJson)

private fun parsePairs(raw: String?): List<SentencePair> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching { json.decodeFromString<List<SentencePair>>(raw) }.getOrElse { emptyList() }
}

fun CardEntity.toFsrsCard(): FsrsCard = FsrsCard(
    cardId = id,
    state = CardState.fromValue(state),
    step = step,
    stability = stability,
    difficulty = difficulty,
    due = Instant.ofEpochMilli(dueAt),
    lastReview = lastReviewAt?.let { Instant.ofEpochMilli(it) },
)

fun WordEntity.displayPhonetic(): String? = phoneticUs ?: phoneticUk

/** 释义按行拆开，去掉空行 */
fun WordEntity.translationLines(): List<String> =
    transCn?.split("\n")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
