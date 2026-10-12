package com.wordbook.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wordbook.data.assets.AssetImporter
import com.wordbook.data.db.AppDatabase
import com.wordbook.data.db.CardEntity
import com.wordbook.data.dict.DictRepository
import com.wordbook.data.prefs.SettingsRepository
import com.wordbook.data.repo.StudyRepository
import com.wordbook.data.repo.StudyWord
import com.wordbook.domain.fsrs.Rating
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 「今日已复习」这类统计数字必须按**卡片张数**算，而不是按评分次数算。
 * 一张卡点「重来」会重新入队，再评一次就是两条日志 —— 但它仍然只是一张卡。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReviewCountTest {

    private lateinit var db: AppDatabase
    private lateinit var study: StudyRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val settings = SettingsRepository(context)
        val importer = AssetImporter(context, db)
        study = StudyRepository(db, settings)
        runBlocking { importer.importWordsIfNeeded() }
    }

    @After
    fun tearDown() = db.close()

    /** 同一张复习卡评两次（重来 → 良好），「今日已复习」应该还是 1 */
    @Test
    fun 同一张卡评两次只算一张() = runBlocking {
        val word = db.wordDao().byHeadword("paragraph")!!
        val cardId = db.cardDao().insert(
            CardEntity(
                wordId = word.id,
                state = 2,
                dueAt = System.currentTimeMillis() - 1000,
                stability = 10.0,
                difficulty = 5.0,
                reps = 3,
            )
        )
        study.rate(StudyWord(word, db.cardDao().byId(cardId)!!), Rating.AGAIN, 1000)
        study.rate(StudyWord(word, db.cardDao().byId(cardId)!!), Rating.GOOD, 1000)

        val logs = db.reviewLogDao().byDay(study.todayKey()).size
        assertEquals("应该产生 2 条日志（复现用）", 2, logs)

        val task = study.todayTask()
        assertEquals("同一张卡评两次，今日已复习应算 1 张", 1, task.reviewDone)
    }

    /** 复习上限也应该按张数扣，而不是按评分次数扣 */
    @Test
    fun 复习上限按卡片张数扣() = runBlocking {
        val settings = SettingsRepository(ApplicationProvider.getApplicationContext())
        settings.setDailyReviewLimit(3)

        val cards = listOf("paragraph", "political", "influence").map { head ->
            val word = db.wordDao().byHeadword(head)!!
            val id = db.cardDao().insert(
                CardEntity(
                    wordId = word.id,
                    state = 2,
                    dueAt = System.currentTimeMillis() - 1000,
                    stability = 10.0,
                    difficulty = 5.0,
                    reps = 3,
                )
            )
            StudyWord(word, db.cardDao().byId(id)!!)
        }
        // 第一张卡评两次（重来 → 良好）：只算消耗 1 个名额
        study.rate(cards[0], Rating.AGAIN, 1000)
        study.rate(StudyWord(cards[0].word, db.cardDao().byId(cards[0].card.id)!!), Rating.GOOD, 1000)

        val session = study.reviewSession()
        assertEquals("上限 3、已复习 1 张，还应给 2 张（按评分次数算只会给 1 张）", 2, session.size)
    }
}
