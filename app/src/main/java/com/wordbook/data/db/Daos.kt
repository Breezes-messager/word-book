package com.wordbook.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<WordEntity>)

    @Query("SELECT COUNT(*) FROM words")
    suspend fun count(): Long

    @Query("DELETE FROM words")
    suspend fun deleteAll()

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun byId(id: Long): WordEntity?

    /** 词书数据升级时，只补新字段，不动卡片关联（id 不变，复习进度安全） */
    @Query(
        "UPDATE words SET headword = :headword, phoneticUs = :phoneticUs, phoneticUk = :phoneticUk, " +
            "transCn = :transCn, transEn = :transEn, examplesJson = :examplesJson, " +
            "phrasesJson = :phrasesJson, remMethod = :remMethod, synoJson = :synoJson, " +
            "relWordJson = :relWordJson WHERE id = :id"
    )
    suspend fun updateDetails(
        id: Long,
        headword: String,
        phoneticUs: String?,
        phoneticUk: String?,
        transCn: String?,
        transEn: String?,
        examplesJson: String?,
        phrasesJson: String?,
        remMethod: String?,
        synoJson: String?,
        relWordJson: String?,
    )

    @Query("SELECT id FROM words")
    suspend fun allIds(): List<Long>

    /** 导入备份时用：headword -> id 的映射 */
    @Query("SELECT id, headword FROM words")
    suspend fun idAndHeadwords(): List<WordIdName>

    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<WordEntity>

    @Query("SELECT * FROM words WHERE headword = :headword COLLATE NOCASE LIMIT 1")
    suspend fun byHeadword(headword: String): WordEntity?

    @Query("SELECT * FROM words WHERE headword LIKE :prefix || '%' COLLATE NOCASE ORDER BY rank LIMIT :limit")
    suspend fun searchPrefix(prefix: String, limit: Int): List<WordEntity>

    /**
     * 搜索：英文前缀 + 中文释义，结果带上卡片状态（用于界面上的学习状态徽章）。
     *
     * 排序规则：英文前缀命中的排前面（更精确）→ 用户手动「加入今日学习」的排前面
     * → 其余按词频（rank）。**同名参数 :q 出现多次是合法的**，Room 只绑定一次。
     */
    @Query(
        """
        SELECT w.id AS id, w.headword AS headword, w.phoneticUs AS phoneticUs,
               w.phoneticUk AS phoneticUk, w.transCn AS transCn, w.transEn AS transEn,
               w.remMethod AS remMethod, w.rank AS rank,
               c.state AS state, c.dueAt AS dueAt, c.lapses AS lapses
        FROM words w LEFT JOIN cards c ON c.wordId = w.id
        WHERE w.headword LIKE :q || '%' COLLATE NOCASE
           OR w.transCn LIKE '%' || :q || '%'
        ORDER BY
            CASE WHEN w.headword LIKE :q || '%' COLLATE NOCASE THEN 0 ELSE 1 END,
            CASE WHEN c.dueAt = 0 THEN 0 ELSE 1 END,
            w.rank ASC
        LIMIT :limit
        """
    )
    suspend fun search(q: String, limit: Int): List<WordSearchRow>

    /** 四个筛选标签的数量，一次查询全算出来（避免 4 次全表扫描） */
    @Query(
        """
        SELECT COALESCE(COUNT(*), 0) AS total,
               COALESCE(SUM(CASE WHEN c.id IS NULL OR c.state = 0 THEN 1 ELSE 0 END), 0) AS unlearned,
               COALESCE(SUM(CASE WHEN c.id IS NOT NULL AND c.state != 0 THEN 1 ELSE 0 END), 0) AS learned,
               COALESCE(SUM(CASE WHEN c.lapses >= 3 THEN 1 ELSE 0 END), 0) AS hard
        FROM words w LEFT JOIN cards c ON c.wordId = w.id
        WHERE w.headword LIKE :q || '%' COLLATE NOCASE
           OR w.transCn LIKE '%' || :q || '%'
        """
    )
    suspend fun searchCounts(q: String): WordSearchCounts

    @Query("SELECT * FROM words ORDER BY deckPriority, rank LIMIT :limit OFFSET :offset")
    suspend fun page(limit: Int, offset: Int): List<WordEntity>

    @Query("SELECT * FROM words ORDER BY deckPriority, rank LIMIT :limit")
    suspend fun firstWords(limit: Int): List<WordEntity>

    @Query("SELECT deck, COUNT(*) AS total FROM words GROUP BY deck")
    suspend fun countByDeck(): List<DeckCount>

    /** 还没学过的新词数量 */
    @Query(
        """
        SELECT COUNT(*) FROM words w
        LEFT JOIN cards c ON c.wordId = w.id
        WHERE c.id IS NULL OR c.state = 0
        """
    )
    suspend fun countNewAvailable(): Int

    /**
     * 顺序模式：还没建卡的词，按词书优先级 + 序号取。
     * `c.dueAt = 0` 是"用户手动加入今日学习"的约定值，这些词排最前面。
     */
    @Query(
        """
        SELECT w.* FROM words w
        LEFT JOIN cards c ON c.wordId = w.id
        WHERE c.id IS NULL OR c.state = 0
        ORDER BY CASE WHEN c.dueAt = 0 THEN 0 ELSE 1 END, w.deckPriority ASC, w.rank ASC
        LIMIT :limit
        """
    )
    suspend fun newWordsSequential(limit: Int): List<WordEntity>

    /** 乱序模式：还没建卡的词，按稳定的乱序键取（手动加入的同样排最前） */
    @Query(
        """
        SELECT w.* FROM words w
        LEFT JOIN cards c ON c.wordId = w.id
        WHERE c.id IS NULL OR c.state = 0
        ORDER BY CASE WHEN c.dueAt = 0 THEN 0 ELSE 1 END, w.shuffleKey ASC
        LIMIT :limit
        """
    )
    suspend fun newWordsShuffled(limit: Int): List<WordEntity>
}

data class DeckCount(val deck: String, val total: Int)

data class WordIdName(val id: Long, val headword: String)

/** 搜索结果行：词条 + 卡片状态（state=null 表示还没有卡片，即"未学"） */
data class WordSearchRow(
    val id: Long,
    val headword: String,
    val phoneticUs: String?,
    val phoneticUk: String?,
    val transCn: String?,
    val transEn: String?,
    val remMethod: String?,
    val rank: Int,
    val state: Int?,
    val dueAt: Long?,
    val lapses: Int?,
)

/** 搜索结果的四个筛选数量 */
data class WordSearchCounts(
    val total: Int = 0,
    val unlearned: Int = 0,
    val learned: Int = 0,
    val hard: Int = 0,
)

@Dao
interface CardDao {
    @Insert
    suspend fun insert(card: CardEntity): Long

    @Insert
    suspend fun insertAll(cards: List<CardEntity>)

    /** 批量建卡并按输入顺序返回自增 id（"不限"时一次要建上百张） */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllReturningIds(cards: List<CardEntity>): List<Long>

    @Query("SELECT * FROM cards WHERE wordId IN (:wordIds)")
    suspend fun byWordIds(wordIds: List<Long>): List<CardEntity>

    @Update
    suspend fun update(card: CardEntity)

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun byId(id: Long): CardEntity?

    @Query("SELECT * FROM cards WHERE wordId = :wordId")
    suspend fun byWordId(wordId: Long): CardEntity?

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM cards WHERE state = :state")
    suspend fun countByState(state: Int): Int

    /** 到期卡片：所有 due <= now 且已开始学习的卡，按到期时间升序 */
    @Query(
        """
        SELECT * FROM cards
        WHERE dueAt <= :now AND state != 0
        ORDER BY dueAt ASC
        LIMIT :limit
        """
    )
    suspend fun dueCards(now: Long, limit: Int): List<CardEntity>

    @Query("SELECT COUNT(*) FROM cards WHERE dueAt <= :now AND state != 0")
    suspend fun countDue(now: Long): Int

    @Query("SELECT * FROM cards WHERE state != 0 ORDER BY lastReviewAt DESC")
    suspend fun learnedCards(): List<CardEntity>

    @Query("DELETE FROM cards")
    suspend fun deleteAll()

    /** 把某张卡改成"今天要学"：dueAt = 0 是手动加入的约定值（新词队列会把它排到最前） */
    @Query("UPDATE cards SET dueAt = 0 WHERE id = :id")
    suspend fun markDueNow(id: Long)
}

@Dao
interface ReviewLogDao {
    @Insert
    suspend fun insert(log: ReviewLogEntity)

    @Query("SELECT * FROM review_logs WHERE dayKey = :dayKey ORDER BY reviewedAt DESC")
    suspend fun byDay(dayKey: String): List<ReviewLogEntity>

    @Query("SELECT COUNT(*) FROM review_logs WHERE dayKey = :dayKey AND stateBefore = 0")
    suspend fun newCountByDay(dayKey: String): Int

    @Query("SELECT COUNT(*) FROM review_logs WHERE dayKey = :dayKey AND stateBefore != 0")
    suspend fun reviewCountByDay(dayKey: String): Int

    @Query("SELECT COUNT(*) FROM review_logs WHERE dayKey = :dayKey")
    suspend fun totalCountByDay(dayKey: String): Int

    @Query("SELECT COUNT(*) FROM review_logs")
    suspend fun totalCount(): Int

    @Query("SELECT DISTINCT dayKey FROM review_logs ORDER BY dayKey DESC")
    suspend fun distinctDayKeys(): List<String>

    @Query("SELECT DISTINCT wordId FROM review_logs WHERE dayKey = :dayKey ORDER BY reviewedAt ASC")
    suspend fun studiedWordIdsByDay(dayKey: String): List<Long>

    @Query("SELECT * FROM review_logs ORDER BY reviewedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ReviewLogEntity>

    @Query("SELECT COUNT(*) FROM review_logs WHERE rating = 1")
    suspend fun againCount(): Int

    @Query("DELETE FROM review_logs")
    suspend fun clear()
}

@Dao
interface WordContextDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(contexts: List<WordContextEntity>)

    @Query("SELECT * FROM word_contexts WHERE wordId = :wordId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun byWordId(wordId: Long, limit: Int = 3): List<WordContextEntity>

    @Query("SELECT * FROM word_contexts WHERE wordId IN (:wordIds) ORDER BY createdAt DESC")
    suspend fun byWordIds(wordIds: List<Long>): List<WordContextEntity>

    @Query("SELECT COUNT(*) FROM word_contexts")
    suspend fun count(): Int

    @Query("DELETE FROM word_contexts")
    suspend fun clear()
}

@Dao
interface ArticleDao {
    @Insert
    suspend fun insert(article: ArticleEntity): Long

    @Insert
    suspend fun insertWords(words: List<ArticleWordEntity>)

    @Query("SELECT * FROM articles ORDER BY createdAt DESC LIMIT 1")
    suspend fun latest(): ArticleEntity?

    @Query("SELECT * FROM articles WHERE dateKey = :dayKey ORDER BY batchIndex ASC")
    suspend fun byDay(dayKey: String): List<ArticleEntity>

    @Query("SELECT * FROM articles ORDER BY createdAt DESC LIMIT :limit")
    fun recentFlow(limit: Int): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun byId(id: Long): ArticleEntity?

    @Query("SELECT * FROM article_words WHERE articleId = :articleId")
    suspend fun wordsOf(articleId: Long): List<ArticleWordEntity>

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun count(): Int

    @Query("DELETE FROM articles WHERE dateKey = :dayKey")
    suspend fun deleteByDay(dayKey: String)

    @Query("DELETE FROM articles")
    suspend fun deleteAll()

    @Query("DELETE FROM article_words")
    suspend fun deleteAllWords()
}
