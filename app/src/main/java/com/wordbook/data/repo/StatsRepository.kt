package com.wordbook.data.repo

import com.wordbook.data.db.AppDatabase
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** 学习统计数据（全部由本地日志推导） */
@Singleton
class StatsRepository @Inject constructor(db: AppDatabase) {
    private val logDao = db.reviewLogDao()
    private val cardDao = db.cardDao()
    private val wordDao = db.wordDao()
    private val articleDao = db.articleDao()

    /** 累计学过多少张卡（建过卡即算学过） */
    suspend fun learnedTotal(): Int = cardDao.count()

    /** 复习状态分布：new / learning / review / relearning */
    suspend fun stateDistribution(): Map<String, Int> = mapOf(
        "新词" to cardDao.countByState(0),
        "学习中" to cardDao.countByState(1),
        "复习中" to cardDao.countByState(2),
        "重学中" to cardDao.countByState(3),
    )

    suspend fun totalReviews(): Int = logDao.totalCount()

    suspend fun wordBankSize(): Int = wordDao.count().toInt()

    /** 连续打卡天数：从今天（或昨天）往前数连续有复习记录的天数 */
    suspend fun streak(today: LocalDate = LocalDate.now()): Int {
        val days = logDao.distinctDayKeys().toSet()
        if (days.isEmpty()) return 0
        var cursor = if (days.contains(today.toString())) today else today.minusDays(1)
        if (!days.contains(cursor.toString())) return 0
        var streak = 0
        while (days.contains(cursor.toString())) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /** 最近 n 天的每日学习量（用于打卡日历） */
    suspend fun dailyCounts(days: Int, today: LocalDate = LocalDate.now()): List<Pair<String, Int>> {
        val keys = logDao.distinctDayKeys().toSet()
        return (0 until days).map { offset ->
            val day = today.minusDays(offset.toLong()).toString()
            day to (if (keys.contains(day)) logDao.totalCountByDay(day) else 0)
        }.reversed()
    }

    suspend fun recentArticles(limit: Int) = articleDao.recentFlow(limit)
}
