package com.wordbook.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wordbook.data.db.AppDatabase
import com.wordbook.data.db.MIGRATION_1_2
import com.wordbook.data.db.MIGRATION_2_3
import com.wordbook.data.db.MIGRATION_3_4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * 数据库迁移测试。
 *
 * 老用户升级时不能丢数据。这里不走 Room 的 MigrationTestHelper（它要求 schema 文件出现在
 * assets 里，Robolectric 下读不到），而是**用 v1 的真实建表语句手造一个老库**，
 * 再用带迁移的 Room 打开它 —— 只要有一条 ALTER 写错，Room 的校验就会拒绝打开。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** 来自 app/schemas/…/1.json，原样抄下来 */
    private val v1Schema = listOf(
        "CREATE TABLE IF NOT EXISTS `words` (`id` INTEGER NOT NULL, `headword` TEXT NOT NULL, " +
            "`phoneticUs` TEXT, `phoneticUk` TEXT, `transCn` TEXT, `transEn` TEXT, " +
            "`examplesJson` TEXT, `phrasesJson` TEXT, `rank` INTEGER NOT NULL, `deck` TEXT NOT NULL, " +
            "`deckPriority` INTEGER NOT NULL, `shuffleKey` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_words_headword` ON `words` (`headword`)",
        "CREATE INDEX IF NOT EXISTS `index_words_deckPriority_rank` ON `words` (`deckPriority`, `rank`)",
        "CREATE INDEX IF NOT EXISTS `index_words_shuffleKey` ON `words` (`shuffleKey`)",
        "CREATE TABLE IF NOT EXISTS `cards` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`wordId` INTEGER NOT NULL, `state` INTEGER NOT NULL, `step` INTEGER, `dueAt` INTEGER NOT NULL, " +
            "`stability` REAL, `difficulty` REAL, `reps` INTEGER NOT NULL, `lapses` INTEGER NOT NULL, " +
            "`lastReviewAt` INTEGER, `createdAt` INTEGER NOT NULL, " +
            "FOREIGN KEY(`wordId`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_cards_wordId` ON `cards` (`wordId`)",
        "CREATE INDEX IF NOT EXISTS `index_cards_dueAt` ON `cards` (`dueAt`)",
        "CREATE TABLE IF NOT EXISTS `review_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`cardId` INTEGER NOT NULL, `wordId` INTEGER NOT NULL, `rating` INTEGER NOT NULL, " +
            "`reviewedAt` INTEGER NOT NULL, `dayKey` TEXT NOT NULL, `stateBefore` INTEGER NOT NULL, " +
            "`stabilityBefore` REAL, `difficultyBefore` REAL, `stateAfter` INTEGER NOT NULL, " +
            "`stabilityAfter` REAL, `difficultyAfter` REAL, `intervalAfterMs` INTEGER NOT NULL, " +
            "`elapsedDays` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL)",
        "CREATE INDEX IF NOT EXISTS `index_review_logs_cardId` ON `review_logs` (`cardId`)",
        "CREATE INDEX IF NOT EXISTS `index_review_logs_dayKey` ON `review_logs` (`dayKey`)",
        "CREATE INDEX IF NOT EXISTS `index_review_logs_reviewedAt` ON `review_logs` (`reviewedAt`)",
        "CREATE TABLE IF NOT EXISTS `articles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`dateKey` TEXT NOT NULL, `batchIndex` INTEGER NOT NULL, `title` TEXT NOT NULL, " +
            "`titleCn` TEXT NOT NULL, `contentJson` TEXT NOT NULL, `style` TEXT NOT NULL, " +
            "`targetWordCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)",
        "CREATE INDEX IF NOT EXISTS `index_articles_dateKey` ON `articles` (`dateKey`)",
        "CREATE TABLE IF NOT EXISTS `article_words` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`articleId` INTEGER NOT NULL, `wordId` INTEGER, `target` TEXT NOT NULL, `surface` TEXT NOT NULL, " +
            "FOREIGN KEY(`articleId`) REFERENCES `articles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_article_words_articleId` ON `article_words` (`articleId`)",
        "CREATE INDEX IF NOT EXISTS `index_article_words_wordId` ON `article_words` (`wordId`)",
    )

    private fun buildV1Database(name: String): File {
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        file.delete()
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        v1Schema.forEach { db.execSQL(it) }
        db.execSQL(
            "INSERT INTO words (id, headword, rank, deck, deckPriority, shuffleKey) " +
                "VALUES (1, 'energy', 1, 'KaoYan_2', 0, 0)"
        )
        db.execSQL(
            "INSERT INTO cards (wordId, state, dueAt, reps, lapses, createdAt) " +
                "VALUES (1, 2, 1000, 5, 1, 900)"
        )
        db.execSQL(
            "INSERT INTO articles (dateKey, batchIndex, title, titleCn, contentJson, style, targetWordCount, createdAt) " +
                "VALUES ('2026-01-01', 0, 'Old Title', '旧标题', '{}', '小故事', 5, 1000)"
        )
        db.version = 1
        db.close()
        return file
    }

    private fun openMigrated(name: String): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

    /** 迁移到最新版后，老数据必须还在，新字段必须可用 */
    @Test
    fun 从_v1_能看到最新版且不丢数据() = runBlocking {
        val name = "migration-v1.db"
        buildV1Database(name)
        val db = openMigrated(name)
        // 只要迁移写错，Room 打开时就会抛 IllegalStateException
        db.openHelper.writableDatabase

        db.openHelper.readableDatabase.query("SELECT headword FROM words").use { cursor ->
            assertTrue("词条应该保留", cursor.moveToFirst())
            assertEquals("energy", cursor.getString(0))
        }

        val articles = db.articleDao().recent(10)
        assertEquals("文章应该保留", 1, articles.size)
        assertEquals("Old Title", articles.first().title)
        assertNull("老文章默认未读", articles.first().readAt)

        // 新字段能写能读
        db.articleDao().markRead(articles.first().id, 12345L)
        assertEquals(12345L, db.articleDao().recent(10).first().readAt)
        assertTrue("标记后未读数应该为 0", db.articleDao().unreadCount() == 0)

        db.close()
    }

    /** v3 也单独测一条：v2→v3 加的表不能影响 articles */
    @Test
    fun 从_v3_迁到最新版() = runBlocking {
        val name = "migration-v3.db"
        val file = buildV1Database(name)
        // 先把 v1 升到 v3 的中间态：直接补上 v2/v3 的字段与表
        val raw = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
        raw.execSQL("ALTER TABLE words ADD COLUMN remMethod TEXT")
        raw.execSQL("ALTER TABLE words ADD COLUMN synoJson TEXT")
        raw.execSQL("ALTER TABLE words ADD COLUMN relWordJson TEXT")
        raw.execSQL(
            "CREATE TABLE IF NOT EXISTS `word_contexts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`wordId` INTEGER NOT NULL, `sentence` TEXT NOT NULL, `sourceDate` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`wordId`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        raw.execSQL("CREATE INDEX IF NOT EXISTS `index_word_contexts_wordId` ON `word_contexts` (`wordId`)")
        raw.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_word_contexts_wordId_sentence` " +
                "ON `word_contexts` (`wordId`, `sentence`)"
        )
        raw.version = 3
        raw.close()

        // 只跑 v3→v4，其余算「已经迁过了」
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()
        db.openHelper.writableDatabase

        assertEquals(1, db.articleDao().recent(10).size)
        assertEquals(1, db.articleDao().unreadCount())
        db.close()
    }
}
