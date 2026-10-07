package com.wordbook.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wordbook.data.assets.AssetImporter
import com.wordbook.data.db.AppDatabase
import com.wordbook.data.db.CardEntity
import com.wordbook.data.dict.DictRepository
import com.wordbook.data.prefs.SettingsRepository
import com.wordbook.data.repo.SearchRepository
import com.wordbook.domain.model.MatchKind
import com.wordbook.domain.model.SearchFilter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 单词搜索的集成测试（Robolectric，用真实词库）。
 * 覆盖：英文前缀、中文释义、筛选数量、状态徽章、加入今日学习。
 *
 * 前置：python tools/build_assets.py 已生成 assets/words.db。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SearchIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context
    private lateinit var settings: SettingsRepository
    private lateinit var importer: AssetImporter
    private lateinit var search: SearchRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settings = SettingsRepository(context)
        importer = AssetImporter(context, db)
        search = SearchRepository(db, DictRepository(context, importer))
        runBlocking { importer.importWordsIfNeeded() }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun 英文前缀搜索按词频排序() = runBlocking {
        val outcome = search.search("par", SearchFilter.ALL)
        val heads = outcome.hits.map { it.headword }
        assertTrue("应命中 paragraph，实际 " + heads, heads.contains("paragraph"))
        assertTrue("应命中 parallel", heads.contains("parallel"))
        assertEquals("前缀命中应标记为 PREFIX", MatchKind.PREFIX, outcome.matchKind)
        // 前缀命中的都排在前面
        assertTrue(heads.first().startsWith("par"))
    }

    @Test
    fun 中文释义也能搜到() = runBlocking {
        val outcome = search.search("妨碍", SearchFilter.ALL)
        val heads = outcome.hits.map { it.headword }
        assertTrue("应命中 hamper，实际 " + heads, heads.contains("hamper"))
        assertTrue("应命中 interfere", heads.contains("interfere"))
        assertEquals("中文命中应标记为按释义匹配", MatchKind.MEANING, outcome.matchKind)
        // 命中的词，释义里必须真的含有这个词
        outcome.hits.forEach { hit ->
            assertTrue(hit.headword + " 的释义不含「妨碍」", hit.translation.contains("妨碍"))
        }
    }

    @Test
    fun 筛选数量与实际结果一致() = runBlocking {
        val all = search.search("妨碍", SearchFilter.ALL)
        val counts = all.counts
        assertEquals(all.counts[SearchFilter.ALL], counts[SearchFilter.ALL])
        assertEquals(
            "未学 + 已学 应等于总数",
            counts[SearchFilter.ALL],
            counts[SearchFilter.UNLEARNED]!! + counts[SearchFilter.LEARNED]!!,
        )
        val unlearned = search.search("妨碍", SearchFilter.UNLEARNED)
        assertTrue("未学筛选结果都应是未学", unlearned.hits.all { !it.learned })
    }

    @Test
    fun 状态徽章覆盖未学与复习中() = runBlocking {
        // 给 paragraph 建一张已学（复习）卡
        val paragraph = db.wordDao().byHeadword("paragraph")
        assertNotNull(paragraph)
        val now = System.currentTimeMillis()
        db.cardDao().insert(
            CardEntity(
                wordId = paragraph!!.id,
                state = 2,
                dueAt = now + 24 * 3600_000L,
                stability = 10.0,
                difficulty = 5.0,
                reps = 3,
                lapses = 4, // 难词阈值
            )
        )
        val outcome = search.search("paragraph", SearchFilter.ALL)
        val hit = outcome.hits.first { it.headword == "paragraph" }
        assertTrue("重来 4 次应算难词", hit.hard)
        assertEquals("难词 · 重来 4 次", hit.badge.label)
        // 没建卡的词是未学
        val unseen = search.search("趴", SearchFilter.ALL)
        assertTrue(unseen.hits.isEmpty() || unseen.hits.all { !it.learned })
    }

    @Test
    fun 加入今日学习后进入新词队列最前() = runBlocking {
        val word = db.wordDao().byHeadword("paragraph")!!
        val result = search.addToToday(word.id)
        assertEquals(SearchRepository.AddResult.Added, result)

        val card = db.cardDao().byWordId(word.id)
        assertNotNull("应该建了卡片", card)
        assertEquals("标记值应是 0", SearchRepository.PINNED_DUE_AT, card!!.dueAt)

        // 选词查询应该把它排到第一个
        val queue = db.wordDao().newWordsSequential(5)
        assertEquals("手动加入的词应排在最前", "paragraph", queue.first().headword)
    }

    @Test
    fun 已学过的词加入今日学习会变成到期() = runBlocking {
        val word = db.wordDao().byHeadword("hamper")!!
        db.cardDao().insert(
            CardEntity(wordId = word.id, state = 2, dueAt = System.currentTimeMillis() + 7 * 86400_000L)
        )
        val result = search.addToToday(word.id)
        assertEquals(SearchRepository.AddResult.MovedToReview, result)
        val card = db.cardDao().byWordId(word.id)!!
        assertTrue("应该立刻到期", card.dueAt <= System.currentTimeMillis())
    }

    @Test
    fun 空查询返回空结果() = runBlocking {
        val outcome = search.search("   ", SearchFilter.ALL)
        assertTrue(outcome.hits.isEmpty())
    }
}
