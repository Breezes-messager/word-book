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

    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<WordEntity>

    @Query("SELECT * FROM words WHERE headword = :headword COLLATE NOCASE LIMIT 1")
    suspend fun byHeadword(headword: String): WordEntity?

    @Query("SELECT * FROM words WHERE headword LIKE :prefix || '%' COLLATE NOCASE ORDER BY rank LIMIT :limit")
    suspend fun searchPrefix(prefix: String, limit: Int): List<WordEntity>

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

    /** 顺序模式：还没建卡的词，按词书优先级 + 序号取 */
    @Query(
        """
        SELECT w.* FROM words w
        LEFT JOIN cards c ON c.wordId = w.id
        WHERE c.id IS NULL OR c.state = 0
        ORDER BY w.deckPriority ASC, w.rank ASC
        LIMIT :limit
        """
    )
    suspend fun newWordsSequential(limit: Int): List<WordEntity>

    /** 乱序模式：还没建卡的词，按稳定的乱序键取 */
    @Query(
        """
        SELECT w.* FROM words w
        LEFT JOIN cards c ON c.wordId = w.id
        WHERE c.id IS NULL OR c.state = 0
        ORDER BY w.shuffleKey ASC
        LIMIT :limit
        """
    )
    suspend fun newWordsShuffled(limit: Int): List<WordEntity>
}

data class DeckCount(val deck: String, val total: Int)

@Dao
interface CardDao {
    @Insert
    suspend fun insert(card: CardEntity): Long

    @Insert
    suspend fun insertAll(cards: List<CardEntity>)

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
