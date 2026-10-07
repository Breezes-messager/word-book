package com.wordbook.data.repo

import com.wordbook.data.db.AppDatabase
import com.wordbook.data.db.CardEntity
import com.wordbook.data.db.WordSearchRow
import com.wordbook.data.dict.DictLookupResult
import com.wordbook.data.dict.DictRepository
import com.wordbook.domain.model.MatchKind
import com.wordbook.domain.model.SearchFilter
import com.wordbook.domain.model.SearchHit
import com.wordbook.domain.model.SearchOutcome
import com.wordbook.domain.model.WordBadge
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 单词搜索：英文前缀 + 中文释义。
 *
 * 全部本地查询（词库就 5046 行，中文 LIKE 全表扫也在 10~20ms 量级），不联网。
 * 结果里带上卡片状态，界面据此显示「未学 / 学习中 / 复习中·明天 / 难词」徽章。
 */
@Singleton
class SearchRepository @Inject constructor(
    db: AppDatabase,
    private val dictRepository: DictRepository,
) {
    private val wordDao = db.wordDao()
    private val cardDao = db.cardDao()

    companion object {
        const val MAX_RESULTS = 200
        /** 「加入今日学习」的标记值：dueAt = 0 让选词查询把它排到最前 */
        const val PINNED_DUE_AT = 0L
        /** 重来几次算"难词" */
        const val HARD_LAPSES = 3
    }

    suspend fun search(rawQuery: String, filter: SearchFilter): SearchOutcome {
        val query = rawQuery.trim()
        if (query.isEmpty()) {
            return SearchOutcome(emptyList(), emptyMap(), MatchKind.PREFIX)
        }

        var rows = wordDao.search(query, MAX_RESULTS)
        var matchKind = MatchKind.PREFIX
        var lemma: String? = null

        // 没有前缀命中时，试一次词形还原：running → run
        if (rows.isEmpty() && query.length >= 3 && query.all { it.isLetter() || it == '-' }) {
            val base = (dictRepository.lookup(query) as? DictLookupResult.Lemmatized)?.lemma
            if (base != null && !base.equals(query, ignoreCase = true)) {
                val lemmaRows = wordDao.search(base, MAX_RESULTS)
                if (lemmaRows.isNotEmpty()) {
                    rows = lemmaRows
                    lemma = base
                }
            }
        }

        val counts = wordDao.searchCounts(query)
        // 中文命中为主时，界面写「按释义匹配」
        if (rows.isNotEmpty()) {
            val prefixHit = rows.any { it.headword.startsWith(query, ignoreCase = true) }
            matchKind = if (prefixHit) MatchKind.PREFIX else MatchKind.MEANING
        }

        val now = System.currentTimeMillis()
        val hits = rows.mapNotNull { row -> toHit(row, now) }
        val filtered = hits.filter { hit ->
            when (filter) {
                SearchFilter.ALL -> true
                SearchFilter.UNLEARNED -> !hit.learned
                SearchFilter.LEARNED -> hit.learned
                SearchFilter.HARD -> hit.hard
            }
        }
        return SearchOutcome(
            hits = filtered,
            counts = mapOf(
                SearchFilter.ALL to counts.total,
                SearchFilter.UNLEARNED to counts.unlearned,
                SearchFilter.LEARNED to counts.learned,
                SearchFilter.HARD to counts.hard,
            ),
            matchKind = matchKind,
            lemma = lemma,
        )
    }

    private fun toHit(row: WordSearchRow, now: Long): SearchHit? {
        val learned = row.state != null && row.state != 0
        val lapses = row.lapses ?: 0
        val hard = learned && lapses >= HARD_LAPSES
        val badge = when {
            !learned -> WordBadge("未学", "new")
            hard -> WordBadge("难词 · 重来 " + lapses + " 次", "hard")
            row.state == 2 -> WordBadge("复习中 · " + dueLabel(row.dueAt, now), "due")
            else -> WordBadge("学习中", "learn")
        }
        return SearchHit(
            wordId = row.id,
            headword = row.headword,
            phonetic = (row.phoneticUs ?: row.phoneticUk)?.takeIf { it.isNotBlank() },
            translation = (row.transCn ?: "").replace("\n", "\n").trim(),
            badge = badge,
            learned = learned,
            hard = hard,
        )
    }

    /** 到期时间的人话描述：现在 / 今天 / 明天 / N 天后 */
    private fun dueLabel(dueAt: Long?, now: Long): String {
        if (dueAt == null) return "待安排"
        if (dueAt <= now) return "现在"
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val dueDay = Instant.ofEpochMilli(dueAt).atZone(zone).toLocalDate()
        val days = ChronoUnit.DAYS.between(today, dueDay)
        return when {
            days <= 0L -> "今天"
            days == 1L -> "明天"
            days < 7L -> days.toString() + " 天后"
            else -> (days / 7).toString() + " 周后"
        }
    }

    /**
     * 加入今日学习。
     * 统一用 `dueAt = 0` 表达"现在就学"：
     * - 还没卡片的 → 建卡并置顶到新词队列最前
     * - 还没学（state=0）→ 同样置顶
     * - 已在学/复习中的 → 直接到期，下一次「开始复习」就会出现
     */
    suspend fun addToToday(wordId: Long): AddResult {
        val existing = cardDao.byWordId(wordId)
        if (existing == null) {
            val fresh = CardEntity(wordId = wordId, dueAt = PINNED_DUE_AT)
            fresh.copy(id = cardDao.insert(fresh))
            return AddResult.Added
        }
        cardDao.markDueNow(existing.id)
        return if (existing.state == 0) AddResult.Added else AddResult.MovedToReview
    }

    enum class AddResult { Added, MovedToReview }
}
