package com.wordbook.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.wordbook.data.db.AppDatabase
import com.wordbook.data.prefs.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ExportCard(
    val word: String,
    val state: Int,
    val dueAt: Long,
    val stability: Double? = null,
    val difficulty: Double? = null,
    val reps: Int = 0,
    val lapses: Int = 0,
    val lastReviewAt: Long? = null,
)

@Serializable
data class ExportReviewLog(
    val word: String,
    val rating: Int,
    val reviewedAt: Long,
    val stateBefore: Int,
    val stabilityBefore: Double? = null,
    val difficultyBefore: Double? = null,
    val stateAfter: Int,
    val stabilityAfter: Double? = null,
    val difficultyAfter: Double? = null,
    val intervalAfterMs: Long,
    val elapsedDays: Long,
    val durationMs: Long,
)

@Serializable
data class ExportBundle(
    val exportedAt: Long,
    val version: Int = 1,
    val cards: List<ExportCard> = emptyList(),
    val reviewLogs: List<ExportReviewLog> = emptyList(),
)

/** 导出 / 清空全部数据 */
@Singleton
class DataExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val settingsRepository: SettingsRepository,
) {
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    /** 导出进度与复习日志到应用外部目录，返回文件 */
    suspend fun export(): File = withContext(Dispatchers.IO) {
        val words = db.wordDao().byIds(db.cardDao().learnedCards().map { it.wordId }).associateBy { it.id }
        val cards = db.cardDao().learnedCards().map { card ->
            ExportCard(
                word = words[card.wordId]?.headword ?: card.wordId.toString(),
                state = card.state,
                dueAt = card.dueAt,
                stability = card.stability,
                difficulty = card.difficulty,
                reps = card.reps,
                lapses = card.lapses,
                lastReviewAt = card.lastReviewAt,
            )
        }
        val logs = db.reviewLogDao().recent(100_000).map { log ->
            ExportReviewLog(
                word = words[log.wordId]?.headword ?: log.wordId.toString(),
                rating = log.rating,
                reviewedAt = log.reviewedAt,
                stateBefore = log.stateBefore,
                stabilityBefore = log.stabilityBefore,
                difficultyBefore = log.difficultyBefore,
                stateAfter = log.stateAfter,
                stabilityAfter = log.stabilityAfter,
                difficultyAfter = log.difficultyAfter,
                intervalAfterMs = log.intervalAfterMs,
                elapsedDays = log.elapsedDays,
                durationMs = log.durationMs,
            )
        }
        val bundle = ExportBundle(System.currentTimeMillis(), 1, cards, logs)
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, "wordbook-export-" + stamp + ".json")
        file.writeText(json.encodeToString(ExportBundle.serializer(), bundle))
        file
    }

    /** 清空业务数据（词书 assets 保留，下次启动会重新导入） */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.articleDao().deleteAllWords()
            db.articleDao().deleteAll()
            db.reviewLogDao().clear()
            db.cardDao().deleteAll()
            db.wordDao().deleteAll()
        }
        settingsRepository.clearAll()
    }

    /** 导出文件的分享 Intent（走 FileProvider） */
    fun shareIntent(file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(context, "com.wordbook.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
