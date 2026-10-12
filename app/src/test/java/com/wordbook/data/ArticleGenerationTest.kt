package com.wordbook.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wordbook.data.assets.AssetImporter
import com.wordbook.data.db.AppDatabase
import com.wordbook.data.db.CardEntity
import com.wordbook.data.prefs.SettingsRepository
import com.wordbook.data.remote.ArticleGenerator
import com.wordbook.data.remote.ChatChoice
import com.wordbook.data.remote.ChatMessage
import com.wordbook.data.remote.ChatRequest
import com.wordbook.data.remote.ChatResponse
import com.wordbook.data.remote.DeepSeekApi
import com.wordbook.data.repo.ArticleRepository
import com.wordbook.data.repo.StudyRepository
import com.wordbook.data.repo.StudyWord
import com.wordbook.domain.fsrs.Rating
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 每日文章的「替换语义」。
 *
 * 用户报过「75 篇未读」：同一天的文章会越攒越多。
 * 根因是"删不删旧文章"由**界面状态**决定（`regenerate = state.hasTodayArticle`）——
 * App 刚启动、load() 还没跑完时状态里是空的，于是走了"追加"分支。
 * 现在替换语义固定在仓库层，这个测试把它锁死。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArticleGenerationTest {

    private lateinit var db: AppDatabase
    private lateinit var study: StudyRepository
    private lateinit var articles: ArticleRepository

    /** 假 API：每次都返回同一篇格式合法的文章，不碰网络 */
    private val fakeApi = object : DeepSeekApi {
        var calls = 0
        override suspend fun chatCompletions(
            authorization: String,
            request: ChatRequest,
        ): ChatResponse {
            calls++
            val body = """
                {
                  "title": "Test Article $calls",
                  "titleCn": "测试文章",
                  "paragraphs": ["First paragraph about the topic.", "Second paragraph."],
                  "occurrences": [],
                  "translation": ["第一段中文。", "第二段中文。"]
                }
            """.trimIndent()
            return ChatResponse(
                id = "test",
                model = "deepseek-chat",
                choices = listOf(ChatChoice(index = 0, message = ChatMessage("assistant", body))),
            )
        }
    }

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val settings = SettingsRepository(context)
        // 假 API 不看 Key，但设置里得有值，否则 generate() 会先抛"没填 Key"
        runBlocking { settings.setApiKey("test-key") }
        study = StudyRepository(db, settings)
        articles = ArticleRepository(
            db = db,
            studyRepository = study,
            settingsRepository = settings,
            generator = ArticleGenerator(api = fakeApi, json = Json { ignoreUnknownKeys = true }),
        )
        runBlocking { AssetImporter(context, db).importWordsIfNeeded() }
    }

    @After
    fun tearDown() = db.close()

    /** 今天学过 5 个词 → 正好够生成 1 篇 */
    private fun studyFiveWordsToday() = runBlocking {
        listOf("paragraph", "political", "influence", "intellectual", "science").forEach { head ->
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
            study.rate(StudyWord(word, db.cardDao().byId(id)!!), Rating.GOOD, 1000)
        }
    }

    @Test
    fun 同一天重复生成会替换而不是累积() = runBlocking {
        studyFiveWordsToday()
        val plan = articles.planBatches(articles.todayStudiedWords())
        assertEquals("5 个词应该规划成 1 篇", 1, plan.size)

        val first = articles.generateToday()
        assertEquals("第一次应生成 1 篇", 1, first.savedCount)
        assertEquals(1, articles.todayArticles().size)

        val second = articles.generateToday()
        assertEquals("第二次也应生成 1 篇", 1, second.savedCount)
        assertEquals("同一天生成两次，当天文章数必须还是 1（不能累积成 2）", 1, articles.todayArticles().size)

        // 全库也只有 1 篇
        assertEquals(1, articles.recentArticlesNow(50).size)
        // 而且留下的是新生成的那篇
        assertEquals("Test Article 2", articles.todayArticles().first().title)
    }

    /** 早期版本重复生成留下的「同一天同一批次」垃圾要能清掉 */
    @Test
    fun 清理重复文章只保留每天每批次最新的一份() = runBlocking {
        val day = "2026-10-01"
        // 同一天第 0 篇被生成了 3 次（模拟旧版本的追加 bug）
        listOf(1000L, 2000L, 3000L).forEach { ts ->
            db.articleDao().insert(
                com.wordbook.data.db.ArticleEntity(
                    dateKey = day, batchIndex = 0, title = "Old " + ts, titleCn = "旧",
                    contentJson = "{}", style = "小故事", targetWordCount = 5, createdAt = ts,
                )
            )
        }
        // 同一天第 1 篇 1 次，另一天 1 篇
        db.articleDao().insert(
            com.wordbook.data.db.ArticleEntity(
                dateKey = day, batchIndex = 1, title = "Second", titleCn = "第二",
                contentJson = "{}", style = "小故事", targetWordCount = 5, createdAt = 4000L,
            )
        )
        db.articleDao().insert(
            com.wordbook.data.db.ArticleEntity(
                dateKey = "2026-10-02", batchIndex = 0, title = "Next Day", titleCn = "第二天",
                contentJson = "{}", style = "小故事", targetWordCount = 5, createdAt = 5000L,
            )
        )

        assertEquals(2, articles.cleanupDuplicateArticles())
        val left = articles.recentArticlesNow(20)
        assertEquals("3 篇同日同批次的重复应该只剩 1 篇", 3, left.size)
        assertTrue("留下的应该是最后一次生成的", left.any { it.title == "Old 3000" })
        // 幂等：再跑一次没有变化
        assertEquals(0, articles.cleanupDuplicateArticles())
    }

    @Test
    fun 全部标为已读会清空未读数() = runBlocking {
        studyFiveWordsToday()
        articles.generateToday()
        assertEquals(1, articles.unreadArticleCount())
        assertEquals(1, articles.markAllRead())
        assertEquals(0, articles.unreadArticleCount())
        // 列表里的文章也变成已读
        assertEquals(1, articles.recentArticlesNow(10).count { it.readAt != null })
    }
}
