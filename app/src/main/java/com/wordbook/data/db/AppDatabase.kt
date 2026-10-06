package com.wordbook.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 → v2：词书新增记忆法 / 同近义 / 同根词三个字段（老用户自动补数据，不用重装） */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE words ADD COLUMN remMethod TEXT")
        db.execSQL("ALTER TABLE words ADD COLUMN synoJson TEXT")
        db.execSQL("ALTER TABLE words ADD COLUMN relWordJson TEXT")
    }
}

/** v2 → v3：新增「文章例句回填」表 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `word_contexts` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`wordId` INTEGER NOT NULL, " +
                "`sentence` TEXT NOT NULL, " +
                "`sourceDate` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`wordId`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_word_contexts_wordId` ON `word_contexts` (`wordId`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_word_contexts_wordId_sentence` " +
                "ON `word_contexts` (`wordId`, `sentence`)"
        )
    }
}

@Database(
    entities = [
        WordEntity::class,
        CardEntity::class,
        ReviewLogEntity::class,
        ArticleEntity::class,
        ArticleWordEntity::class,
        WordContextEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun cardDao(): CardDao
    abstract fun reviewLogDao(): ReviewLogDao
    abstract fun articleDao(): ArticleDao
    abstract fun wordContextDao(): WordContextDao

    companion object {
        const val NAME = "wordbook.db"
    }
}
