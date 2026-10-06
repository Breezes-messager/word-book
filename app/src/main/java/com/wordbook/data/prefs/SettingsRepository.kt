package com.wordbook.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wordbook.domain.model.AppSettings
import com.wordbook.domain.model.ArticleStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wordbook_settings")

/**
 * 设置项仓库。API Key 只写在 DataStore 里，绝不写死在源码中。
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val dailyNewWords = intPreferencesKey("daily_new_words")
        val dailyReviewLimit = intPreferencesKey("daily_review_limit")
        val shuffleNewWords = booleanPreferencesKey("shuffle_new_words")
        val articleStyle = stringPreferencesKey("article_style")
        val apiKey = stringPreferencesKey("deepseek_api_key")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val desiredRetention = floatPreferencesKey("desired_retention")
        val translationExpanded = booleanPreferencesKey("translation_expanded")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            dailyNewWords = prefs[Keys.dailyNewWords] ?: 20,
            dailyReviewLimit = prefs[Keys.dailyReviewLimit] ?: 0,
            shuffleNewWords = prefs[Keys.shuffleNewWords] ?: false,
            articleStyle = ArticleStyle.fromLabel(prefs[Keys.articleStyle] ?: ArticleStyle.STORY.label),
            apiKey = prefs[Keys.apiKey] ?: "",
            reminderEnabled = prefs[Keys.reminderEnabled] ?: false,
            reminderHour = prefs[Keys.reminderHour] ?: 20,
            reminderMinute = prefs[Keys.reminderMinute] ?: 0,
            desiredRetention = prefs[Keys.desiredRetention] ?: 0.9f,
            translationExpanded = prefs[Keys.translationExpanded] ?: false,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setDailyNewWords(value: Int) = context.dataStore.edit { it[Keys.dailyNewWords] = value.coerceIn(1, 500) }
    suspend fun setDailyReviewLimit(value: Int) = context.dataStore.edit { it[Keys.dailyReviewLimit] = value.coerceAtLeast(0) }
    suspend fun setShuffleNewWords(value: Boolean) = context.dataStore.edit { it[Keys.shuffleNewWords] = value }
    suspend fun setArticleStyle(style: ArticleStyle) = context.dataStore.edit { it[Keys.articleStyle] = style.label }
    suspend fun setApiKey(key: String) = context.dataStore.edit { it[Keys.apiKey] = key.trim() }
    suspend fun setReminderEnabled(value: Boolean) = context.dataStore.edit { it[Keys.reminderEnabled] = value }
    suspend fun setReminderTime(hour: Int, minute: Int) = context.dataStore.edit {
        it[Keys.reminderHour] = hour.coerceIn(0, 23)
        it[Keys.reminderMinute] = minute.coerceIn(0, 59)
    }
    suspend fun setDesiredRetention(value: Float) = context.dataStore.edit { it[Keys.desiredRetention] = value.coerceIn(0.7f, 0.98f) }
    suspend fun setTranslationExpanded(value: Boolean) = context.dataStore.edit { it[Keys.translationExpanded] = value }

    /** 清空全部设置（含 API Key），用于“清空全部数据” */
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
