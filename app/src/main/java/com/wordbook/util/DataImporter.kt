package com.wordbook.util

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.wordbook.data.db.AppDatabase
import com.wordbook.data.db.ArticleEntity
import com.wordbook.data.db.ArticleWordEntity
import com.wordbook.data.db.CardEntity
import com.wordbook.data.db.ReviewLogEntity
import com.wordbook.data.db.WordContextEntity
import com.wordbook.domain.article.ArticleJsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 导入 [DataExporter] 导出的备份，用于换手机迁移。
 *
 * 语义：**覆盖式导入**（先清空学习数据，再写入备份里的），词库本身不动。
 * 单词按 headword 匹配到本地词库；备份里有但本地词库没有的词会被跳过并计数。
 * 设置与 API Key 不在备份里，导入不会覆盖当前设置。
 */
@Singleton
class DataImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val assetImporter: com.wordbook.data.assets.AssetImporter,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    data class ImportResult(
        val cards: Int,
        val reviewLogs: Int,
        val articles: Int,
        val contexts: Int,
        val skippedWords: Int,
    )

    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().decodeToString()
        } ?: throw IllegalArgumentException("读不到这个文件，换一个试试")

        val bundle = try {
            json.decodeFromString(ExportBundle.serializer(), text)
        } catch (t: Throwable) {
            throw IllegalArgumentException("这不是本 App 导出的备份文件（" + (t.message ?: "解析失败") + "）")
        }

        // 先确保词库在（比如"清空全部数据"之后立刻导入），否则所有词都匹配不上
        assetImporter.importWordsIfNeeded()

        val idByWord = db.wordDao().idAndHeadwords()
            .associate { it.headword.lowercase() to it.id }
        var skipped = 0
        val newCardIdByWord = mutableMapOf<Long, Long>()

        val counts = db.withTransaction {
            // 清空学习数据（保留词库），避免与现有进度混在一起
            db.wordContextDao().clear()
            db.articleDao().deleteAllWords()
            db.articleDao().deleteAll()
            db.reviewLogDao().clear()
            db.cardDao().deleteAll()

            // 1) 卡片
            var cardCount = 0
            bundle.cards.forEach { item ->
                val wordId = idByWord[item.word.lowercase()]
                if (wordId == null) {
                    skipped++
                    return@forEach
                }
                val id = db.cardDao().insert(
                    CardEntity(
                        wordId = wordId,
                        state = item.state,
                        step = item.step,
                        dueAt = item.dueAt,
                        stability = item.stability,
                        difficulty = item.difficulty,
                        reps = item.reps,
                        lapses = item.lapses,
                        lastReviewAt = item.lastReviewAt,
                        createdAt = bundle.exportedAt,
                    )
                )
                newCardIdByWord[wordId] = id
                cardCount++
            }

            // 2) 复习日志
            var logCount = 0
            bundle.reviewLogs.forEach { item ->
                val wordId = idByWord[item.word.lowercase()] ?: return@forEach
                db.reviewLogDao().insert(
                    ReviewLogEntity(
                        cardId = newCardIdByWord[wordId] ?: 0L,
                        wordId = wordId,
                        rating = item.rating,
                        reviewedAt = item.reviewedAt,
                        dayKey = dayKeyOf(item.reviewedAt),
                        stateBefore = item.stateBefore,
                        stabilityBefore = item.stabilityBefore,
                        difficultyBefore = item.difficultyBefore,
                        stateAfter = item.stateAfter,
                        stabilityAfter = item.stabilityAfter,
                        difficultyAfter = item.difficultyAfter,
                        intervalAfterMs = item.intervalAfterMs,
                        elapsedDays = item.elapsedDays,
                        durationMs = item.durationMs,
                    )
                )
                logCount++
            }

            // 3) 文章（原始 JSON 原样恢复，还能继续点词查释义）
            var articleCount = 0
            bundle.articles.forEach { item ->
                val articleId = db.articleDao().insert(
                    ArticleEntity(
                        dateKey = item.dateKey,
                        batchIndex = item.batchIndex,
                        title = item.title,
                        titleCn = item.titleCn,
                        contentJson = item.contentJson,
                        style = item.style,
                        targetWordCount = item.targetWordCount,
                        createdAt = item.createdAt,
                    )
                )
                val content = ArticleJsonParser.fromJsonOrNull(item.contentJson)
                val links = content?.occurrences.orEmpty().mapNotNull { occurrence ->
                    val wordId = idByWord[occurrence.target.lowercase()] ?: return@mapNotNull null
                    ArticleWordEntity(
                        articleId = articleId,
                        wordId = wordId,
                        target = occurrence.target,
                        surface = occurrence.surface,
                    )
                }
                if (links.isNotEmpty()) db.articleDao().insertWords(links)
                articleCount++
            }

            // 4) 文章例句回填
            var contextCount = 0
            val contexts = bundle.contexts.mapNotNull { item ->
                val wordId = idByWord[item.word.lowercase()] ?: return@mapNotNull null
                WordContextEntity(
                    wordId = wordId,
                    sentence = item.sentence,
                    sourceDate = item.sourceDate,
                    createdAt = item.createdAt,
                )
            }
            if (contexts.isNotEmpty()) {
                db.wordContextDao().insertAll(contexts)
                contextCount = contexts.size
            }

            ImportResult(cardCount, logCount, articleCount, contextCount, skipped)
        }
        withContext(Dispatchers.Main) { }
        counts
    }

    private fun dayKeyOf(timestamp: Long): String = DateTimeFormatter.ISO_LOCAL_DATE.format(
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    )
}
