package com.wordbook.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WordEntity::class,
        CardEntity::class,
        ReviewLogEntity::class,
        ArticleEntity::class,
        ArticleWordEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun cardDao(): CardDao
    abstract fun reviewLogDao(): ReviewLogDao
    abstract fun articleDao(): ArticleDao

    companion object {
        const val NAME = "wordbook.db"
    }
}
