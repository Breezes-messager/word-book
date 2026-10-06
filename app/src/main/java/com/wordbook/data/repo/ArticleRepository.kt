package com.wordbook.data.repo

import com.wordbook.data.db.AppDatabase
import com.wordbook.data.db.ArticleEntity
import com.wordbook.data.db.ArticleWordEntity
import com.wordbook.data.db.WordEntity
import com.wordbook.data.prefs.SettingsRepository
import com.wordbook.data.remote.ArticleGenerator
import com.wordbook.domain.article.ArticleContent
import com.wordbook.domain.article.ArticleJsonParser
import com.wordbook.domain.article.ArticlePlan
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val db: AppDatabase,
    private val studyRepository: StudyRepository,
    private val settingsRepository: SettingsRepository,
    private val generator: ArticleGenerator,
) {
    private val articleDao = db.articleDao()

    fun todayKey(): String = LocalDate.now().toString()

    /** 今天学习或复习过的词 */
    suspend fun todayStudiedWords(): List<WordEntity> = studyRepository.wordsStudiedOn(todayKey())

    suspend fun todayArticles(): List<ArticleEntity> = articleDao.byDay(todayKey())

    suspend fun latestArticle(): ArticleEntity? = articleDao.latest()

    fun recentArticles(limit: Int): Flow<List<ArticleEntity>> = articleDao.recentFlow(limit)

    fun contentOf(article: ArticleEntity): ArticleContent? = ArticleJsonParser.fromJsonOrNull(article.contentJson)

    suspend fun articleCount(): Int = articleDao.count()

    /**
     * 把今天的词切成若干篇，每篇不超过 20 个。
     * 词数 5–20 时生成 1 篇（用全部词）；超过 20 时平均分成多篇，避免把词硬塞进一篇。
     */
    fun planBatches(words: List<WordEntity>): List<List<WordEntity>> {
        var cursor = 0
        return ArticlePlan.batchSizes(words.size).map { size ->
            val slice = words.subList(cursor, minOf(cursor + size, words.size))
            cursor += size
            slice
        }
    }

    /**
     * 生成今天的全部文章。每篇生成成功后立刻落盘（Room 存原始 JSON），
     * 某篇失败不影响已经生成的内容。
     */
    suspend fun generateToday(
        regenerate: Boolean = false,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> },
    ): GenerationReport {
        if (regenerate) articleDao.deleteByDay(todayKey())
        val settings = settingsRepository.current()
        val todayWords = todayStudiedWords()
        val batches = planBatches(todayWords)
        if (batches.isEmpty()) {
            return GenerationReport(
                savedCount = 0,
                articles = emptyList(),
                error = "今天学的词太少，攒够 " + ArticlePlan.MIN_WORDS + " 个再来",
            )
        }

        val todayTargets = todayWords.map { it.headword.lowercase() }.toSet()
        val known = studyRepository.knownWords(ArticlePlan.MAX_KNOWN_WORDS)
            .filter { it.headword.lowercase() !in todayTargets }
            .map { it.headword }

        val saved = mutableListOf<ArticleEntity>()
        var failed = 0
        var lastError: String? = null

        batches.forEachIndexed { index, batch ->
            try {
                val content = generator.generate(
                    apiKey = settings.apiKey,
                    targetWords = batch.map { it.headword },
                    knownWords = known,
                    style = settings.articleStyle,
                )
                val entity = saveArticle(content, batch, settings.articleStyle.label, index)
                saved += entity
            } catch (t: Throwable) {
                failed++
                lastError = t.message ?: t.javaClass.simpleName
            }
            onProgress(index + 1, batches.size)
        }

        return GenerationReport(saved.size, saved, lastError, failed)
    }

    data class GenerationReport(
        val savedCount: Int,
        val articles: List<ArticleEntity>,
        val error: String? = null,
        val failedCount: Int = 0,
    )

    private suspend fun saveArticle(
        content: ArticleContent,
        words: List<WordEntity>,
        style: String,
        batchIndex: Int,
    ): ArticleEntity {
        val entity = ArticleEntity(
            dateKey = todayKey(),
            batchIndex = batchIndex,
            title = content.title,
            titleCn = content.titleCn,
            contentJson = ArticleJsonParser.toJson(content),
            style = style,
            targetWordCount = words.size,
            createdAt = System.currentTimeMillis(),
        )
        val id = articleDao.insert(entity)
        val wordIds = words.associate { it.headword.lowercase() to it.id }
        val links = content.occurrences.map { occurrence ->
            ArticleWordEntity(
                articleId = id,
                wordId = wordIds[occurrence.target.lowercase()],
                target = occurrence.target,
                surface = occurrence.surface,
            )
        }
        if (links.isNotEmpty()) articleDao.insertWords(links)
        return entity.copy(id = id)
    }

    /** 手动导入一篇文章（示例 / 测试用），走完全相同的落盘路径 */
    suspend fun saveManual(content: ArticleContent, style: String = "示例"): ArticleEntity {
        val entity = ArticleEntity(
            dateKey = todayKey(),
            batchIndex = 0,
            title = content.title,
            titleCn = content.titleCn,
            contentJson = ArticleJsonParser.toJson(content),
            style = style,
            targetWordCount = content.occurrences.size,
            createdAt = System.currentTimeMillis(),
        )
        val id = articleDao.insert(entity)
        return entity.copy(id = id)
    }

    suspend fun deleteAll() {
        articleDao.deleteAllWords()
        articleDao.deleteAll()
    }
}
