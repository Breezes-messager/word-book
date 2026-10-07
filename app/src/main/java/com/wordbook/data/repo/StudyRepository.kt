package com.wordbook.data.repo

import com.wordbook.data.db.AppDatabase
import com.wordbook.data.db.CardEntity
import com.wordbook.data.db.ReviewLogEntity
import com.wordbook.data.db.WordEntity
import com.wordbook.data.prefs.SettingsRepository
import com.wordbook.domain.fsrs.FsrsReviewResult
import com.wordbook.domain.fsrs.FsrsScheduler
import com.wordbook.domain.fsrs.Rating
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** 一张待学习的词 + 它的卡片 + 它在历史文章里的真实用法 */
data class StudyWord(
    val word: WordEntity,
    val card: CardEntity,
    val contexts: List<String> = emptyList(),
)

/** 首页今日任务 */
data class TodayTask(
    val newTarget: Int,
    val newDone: Int,
    val newAvailable: Int,
    val reviewDue: Int,
    val reviewDone: Int,
    val reviewLimit: Int,
) {
    val newRemaining: Int get() = (newTarget - newDone).coerceAtLeast(0)

    /** reviewDue 是“现在仍到期的卡数”，所以待复习就是它本身 */
    val reviewRemaining: Int get() = reviewDue

    /** 今天一共需要复习多少张：还到期的 + 今天已经复习过的 */
    val reviewTotal: Int get() = reviewDue + reviewDone
}

@Singleton
class StudyRepository @Inject constructor(
    private val db: AppDatabase,
    private val settingsRepository: SettingsRepository,
) {
    private val wordDao = db.wordDao()
    private val cardDao = db.cardDao()
    private val logDao = db.reviewLogDao()
    private val contextDao = db.wordContextDao()

    companion object {
        /** "不限"时单轮会话最多取多少个新词（防止一次建几千张卡） */
        const val UNLIMITED_SESSION_CAP = 200
    }

    /** 批量取"文章例句"，最多每个词 2 句 */
    private suspend fun contextsFor(wordIds: List<Long>): Map<Long, List<String>> {
        if (wordIds.isEmpty()) return emptyMap()
        // 分批查：SQLite 的 IN 参数有上限（老版本 999），"不限"之后一次可能上千个 id
        return wordIds.chunked(500)
            .flatMap { contextDao.byWordIds(it) }
            .groupBy { it.wordId }
            .mapValues { (_, list) -> list.take(2).map { it.sentence } }
    }

    fun todayKey(): String = LocalDate.now().toString()

    suspend fun cardById(id: Long): CardEntity? = cardDao.byId(id)

    /** 每次使用时按当前设置构造排程器（目标保持率可在设置里改） */
    suspend fun scheduler(enableFuzzing: Boolean = true): FsrsScheduler {
        val settings = settingsRepository.current()
        return FsrsScheduler(
            desiredRetention = settings.desiredRetention.toDouble(),
            enableFuzzing = enableFuzzing,
        )
    }

    // ------------------------------------------------------------------ 今日任务

    suspend fun todayTask(): TodayTask {
        val settings = settingsRepository.current()
        val now = System.currentTimeMillis()
        val day = todayKey()

        val newDone = logDao.newCountByDay(day)
        val reviewDone = logDao.reviewCountByDay(day)
        val newAvailable = wordDao.countNewAvailable()
        // 已经复习过的卡 dueAt 已经推到未来，所以这里查到的是“还没复习的”到期卡
        val due = cardDao.countDue(now)
        return TodayTask(
            newTarget = settings.dailyNewWords,
            newDone = newDone,
            newAvailable = newAvailable,
            reviewDue = due,
            reviewDone = reviewDone,
            reviewLimit = settings.dailyReviewLimit,
        )
    }

    // ------------------------------------------------------------------ 会话构造

    /**
     * 新词学习队列。
     * 每日新词数为 0 表示**不限**：不再按"今日已学"扣减，但单轮仍然最多取
     * [UNLIMITED_SESSION_CAP] 个 —— 否则一次要建几千张卡、拼几千个 id 的查询，
     * 启动会明显变慢。学完这一轮可以再点「开始学习」继续拿。
     */
    suspend fun newWordSession(): List<StudyWord> {
        val settings = settingsRepository.current()
        val today = todayKey()
        val done = logDao.newCountByDay(today)
        val dailyLimit = settings.dailyNewWords
        val allowance = if (dailyLimit <= 0) {
            UNLIMITED_SESSION_CAP
        } else {
            (dailyLimit - done).coerceAtLeast(0)
        }
        if (allowance == 0) return emptyList()

        val words = if (settings.shuffleNewWords) {
            wordDao.newWordsShuffled(allowance)
        } else {
            wordDao.newWordsSequential(allowance)
        }

        val now = System.currentTimeMillis()

        // 一次查出这批词已有的卡（原来是每张词一次查询，200 张就是 200 次）
        val existing = words.map { it.id }
            .chunked(500)
            .flatMap { cardDao.byWordIds(it) }
            .associateBy { it.wordId }

        // 缺卡的批量插入（逐张插每张一个事务，200 张要好几秒）
        val missing = words.filter { existing[it.id] == null }
        val fresh: Map<Long, CardEntity> = if (missing.isEmpty()) {
            emptyMap()
        } else {
            val ids = cardDao.insertAllReturningIds(
                missing.map { CardEntity(wordId = it.id, dueAt = now) },
            )
            missing.mapIndexed { index, word ->
                word.id to CardEntity(id = ids[index], wordId = word.id, dueAt = now)
            }.toMap()
        }

        val result = words.map { word ->
            StudyWord(word, existing[word.id] ?: fresh.getValue(word.id))
        }
        val contexts = contextsFor(result.map { it.word.id })
        return result.map { it.copy(contexts = contexts[it.word.id].orEmpty()) }
    }

    /** 复习队列：所有到期的卡，按到期时间升序；每日复习上限由设置决定 */
    suspend fun reviewSession(): List<StudyWord> {
        val settings = settingsRepository.current()
        val day = todayKey()
        val now = System.currentTimeMillis()
        val cards = cardDao.dueCards(now, 100_000)
        val limit = settings.dailyReviewLimit
        val used = logDao.reviewCountByDay(day)
        val allowance = if (limit <= 0) Int.MAX_VALUE else (limit - used).coerceAtLeast(0)
        val selected = if (allowance == Int.MAX_VALUE) cards else cards.take(allowance)
        if (selected.isEmpty()) return emptyList()

        val words = wordDao.byIds(selected.map { it.wordId }).associateBy { it.id }
        val contexts = contextsFor(selected.map { it.wordId })
        return selected.mapNotNull { card ->
            words[card.wordId]?.let { StudyWord(it, card, contexts[card.wordId].orEmpty()) }
        }
    }

    /** 某张卡四档评分的预估间隔，用于按钮上显示 */
    suspend fun previewIntervals(card: CardEntity, now: Instant = Instant.now()): Map<Rating, Duration> =
        scheduler(enableFuzzing = false).previewIntervals(card.toFsrsCard(), now)

    // ------------------------------------------------------------------ 评分

    /**
     * 按 FSRS 评分：更新卡片 + 写入完整复习日志。
     * 日志包含复习前的 state/stability/difficulty 与复习后的间隔，便于日后重新拟合参数。
     */
    suspend fun rate(
        studyWord: StudyWord,
        rating: Rating,
        durationMs: Long,
        now: Instant = Instant.now(),
    ): FsrsReviewResult {
        val scheduler = scheduler(enableFuzzing = true)
        val before = studyWord.card
        val result = scheduler.review(before.toFsrsCard(), rating, now)
        val after = result.card

        val updated = before.copy(
            state = after.state.value,
            step = after.step,
            dueAt = after.due.toEpochMilli(),
            stability = after.stability,
            difficulty = after.difficulty,
            reps = before.reps + 1,
            lapses = before.lapses + if (rating == Rating.AGAIN) 1 else 0,
            lastReviewAt = after.lastReview?.toEpochMilli(),
        )
        cardDao.update(updated)

        logDao.insert(
            ReviewLogEntity(
                cardId = before.id,
                wordId = before.wordId,
                rating = rating.value,
                reviewedAt = now.toEpochMilli(),
                dayKey = todayKey(),
                stateBefore = before.state,
                stabilityBefore = before.stability,
                difficultyBefore = before.difficulty,
                stateAfter = after.state.value,
                stabilityAfter = after.stability,
                difficultyAfter = after.difficulty,
                intervalAfterMs = result.intervalAfterMs,
                elapsedDays = result.elapsedDays ?: -1L,
                durationMs = durationMs,
            )
        )
        return result
    }

    /** 重新把卡片放回今日队列（用于“重来”后立刻再看一次之外的场景，例如手动重置） */
    suspend fun rescheduleNow(card: CardEntity, now: Instant = Instant.now()) {
        cardDao.update(card.copy(dueAt = now.toEpochMilli()))
    }

    /** 最近学过的词（含今天），供文章生成用 */
    suspend fun wordsStudiedOn(dayKey: String): List<WordEntity> {
        val ids = logDao.studiedWordIdsByDay(dayKey)
        if (ids.isEmpty()) return emptyList()
        val words = wordDao.byIds(ids).associateBy { it.id }
        return ids.mapNotNull { words[it] }
    }

    /** 已学过的词（背景词汇表），按 stability 从高到低取最多 limit 个 */
    suspend fun knownWords(limit: Int): List<WordEntity> {
        val cards = cardDao.learnedCards().sortedByDescending { it.stability ?: 0.0 }.take(limit)
        if (cards.isEmpty()) return emptyList()
        val words = wordDao.byIds(cards.map { it.wordId }).associateBy { it.id }
        return cards.mapNotNull { words[it.wordId] }
    }
}
