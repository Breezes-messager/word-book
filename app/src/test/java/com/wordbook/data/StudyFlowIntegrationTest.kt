package com.wordbook.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wordbook.data.assets.AssetImporter
import com.wordbook.data.db.AppDatabase
import com.wordbook.data.dict.DictLookupResult
import com.wordbook.data.dict.DictRepository
import com.wordbook.data.prefs.SettingsRepository
import com.wordbook.data.repo.StudyRepository
import com.wordbook.data.repo.WordRepository
import com.wordbook.domain.fsrs.CardState
import com.wordbook.domain.fsrs.Rating
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
import java.time.Duration
import java.time.Instant

/**
 * 端到端集成测试（Robolectric，跑在 JVM 上）：
 * 用真实的 assets/words.db 与 assets/dict.db，验证
 *   1) 首次启动导入词书
 *   2) 学习 → 评分 → 复习日志 → 今日任务 的闭环
 *   3) 到期卡进入复习队列，间隔符合 FSRS
 *   4) 离线查词与词形还原
 *
 * 前置：先运行 python tools/build_assets.py 生成 assets。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudyFlowIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context
    private lateinit var settings: SettingsRepository
    private lateinit var importer: AssetImporter
    private lateinit var wordRepository: WordRepository
    private lateinit var studyRepository: StudyRepository
    private lateinit var dictRepository: DictRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settings = SettingsRepository(context)
        importer = AssetImporter(context, db)
        wordRepository = WordRepository(db, settings, importer)
        studyRepository = StudyRepository(db, settings)
        dictRepository = DictRepository(context, importer)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `首次启动导入词书并保留原始字段`() = runBlocking {
        val count = importer.importWordsIfNeeded()
        assertTrue("词书应该有几千个词，实际 " + count, count > 4000)
        assertEquals(count, db.wordDao().count().toInt())

        // 再次调用应该幂等
        assertEquals(count, importer.importWordsIfNeeded())

        val first = wordRepository.firstWords(20)
        assertEquals(20, first.size)
        assertTrue("前 20 个词都应该有中文释义", first.all { !it.transCn.isNullOrBlank() })

        val paragraph = wordRepository.byHeadword("paragraph")
        assertNotNull("词库里应该有 paragraph", paragraph)
        assertTrue(!paragraph!!.phoneticUs.isNullOrBlank())
        assertTrue(paragraph.examplesJson!!.contains("paragraphs"))
    }

    @Test
    fun `学新词评分后写入卡片与复习日志`() = runBlocking {
        importer.importWordsIfNeeded()

        val session = studyRepository.newWordSession()
        assertEquals("默认每日新词 20 个", 20, session.size)
        assertTrue("新卡初始状态应为 NEW", session.all { it.card.state == 0 })

        val target = session.first()
        val now = Instant.now()
        val result = studyRepository.rate(target, Rating.GOOD, durationMs = 3200, now = now)

        // FSRS：新卡评“良好”后进入第二个学习步（10 分钟）
        assertEquals(CardState.LEARNING, result.card.state)
        assertEquals(1, result.card.step)
        assertEquals(Duration.ofMinutes(10).toMillis(), result.intervalAfterMs)

        val saved = studyRepository.cardById(target.card.id)
        assertNotNull(saved)
        assertEquals(CardState.LEARNING.value, saved!!.state)
        assertEquals(1, saved.reps)
        assertEquals(0, saved.lapses)
        assertEquals(now.plus(Duration.ofMinutes(10)).toEpochMilli(), saved.dueAt)

        // 复习日志：复习前后的 state / stability / difficulty 都要落库
        val logs = db.reviewLogDao().byDay(studyRepository.todayKey())
        assertEquals(1, logs.size)
        val log = logs.first()
        assertEquals(Rating.GOOD.value, log.rating)
        assertEquals(CardState.NEW.value, log.stateBefore)
        assertEquals(null, log.stabilityBefore)
        assertEquals(CardState.LEARNING.value, log.stateAfter)
        assertTrue("复习后应写入 stability", (log.stabilityAfter ?: 0.0) > 0.0)
        assertTrue("复习后应写入 difficulty", (log.difficultyAfter ?: 0.0) > 0.0)
        assertEquals(Duration.ofMinutes(10).toMillis(), log.intervalAfterMs)
        assertEquals(3200L, log.durationMs)
        assertEquals(-1L, log.elapsedDays)

        // 今日任务：新词已学 1 个
        val task = studyRepository.todayTask()
        assertEquals(1, task.newDone)
        assertEquals(20 - 1, task.newRemaining)
    }

    @Test
    fun `评重来后一分钟内重新到期并计入失误`() = runBlocking {
        importer.importWordsIfNeeded()
        val target = studyRepository.newWordSession().first()
        val now = Instant.now()

        val result = studyRepository.rate(target, Rating.AGAIN, durationMs = 1000, now = now)
        assertEquals(Duration.ofMinutes(1).toMillis(), result.intervalAfterMs)

        val saved = studyRepository.cardById(target.card.id)!!
        assertEquals(1, saved.lapses)
        assertEquals(now.plus(Duration.ofMinutes(1)).toEpochMilli(), saved.dueAt)
        assertTrue("“重来”后到期时间应该在 1 分钟内", saved.dueAt - now.toEpochMilli() <= 60_000)

        // 把 due 改到过去，应该重新进入复习队列
        db.cardDao().update(saved.copy(dueAt = System.currentTimeMillis() - 1000))
        val reviewQueue = studyRepository.reviewSession()
        assertTrue("到期的卡应出现在复习队列里", reviewQueue.any { it.card.id == saved.id })
    }

    @Test
    fun `离线查词支持词形还原与前缀候选`() = runBlocking {
        importer.importWordsIfNeeded()

        val direct = dictRepository.lookup("abandon")
        assertTrue("abandon 应该能查到", direct is DictLookupResult.Exact)

        // 变形词：无论走精确匹配还是词形还原，都要能给出原形的释义
        val running = dictRepository.lookup("Running,")
        val resolved = when (running) {
            is DictLookupResult.Exact -> running.entry
            is DictLookupResult.Lemmatized -> running.entry
            else -> null
        }
        assertNotNull("running, 应该能查到（大小写与标点要清洗掉）", resolved)
        assertTrue("释义不应为空", !resolved!!.translation.isNullOrBlank())

        // 前缀候选
        val prefix = dictRepository.lookup("runn")
        assertTrue(
            "runn 应该给出候选，实际 " + prefix,
            prefix is DictLookupResult.Suggestions || prefix is DictLookupResult.Exact,
        )

        // 查不到的单词要说“未收录”，而不是崩溃
        val missing = dictRepository.lookup("zzzzqqq")
        assertTrue("乱码应该走未收录分支，实际 " + missing, missing is DictLookupResult.NotFound)

        assertTrue("词典条数应大于 1 万", dictRepository.size() > 10_000)
    }
}
