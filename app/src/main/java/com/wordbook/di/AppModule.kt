package com.wordbook.di

import android.content.Context
import androidx.room.Room
import com.wordbook.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .build()

    @Provides
    fun provideWordDao(db: AppDatabase) = db.wordDao()

    @Provides
    fun provideCardDao(db: AppDatabase) = db.cardDao()

    @Provides
    fun provideReviewLogDao(db: AppDatabase) = db.reviewLogDao()

    @Provides
    fun provideArticleDao(db: AppDatabase) = db.articleDao()
}
