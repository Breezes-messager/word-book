package com.wordbook.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wordbook.MainActivity
import com.wordbook.R
import com.wordbook.data.prefs.SettingsRepository
import com.wordbook.data.repo.StudyRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * 每日提醒：到点检查今日任务，用本地通知提醒（纯本地，不联网）。
 * 用 EntryPointAccessors 取依赖，避免为 WorkManager 再配一套 HiltWorkerFactory。
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderEntryPoint::class.java,
        )
        val settings = entryPoint.settingsRepository().current()
        if (!settings.reminderEnabled) return Result.success()

        val task = entryPoint.studyRepository().todayTask()
        val text = "今天还有 " + task.newRemaining + " 个新词、" + task.reviewRemaining + " 张卡待复习"
        notify(text)
        return Result.success()
    }

    private fun notify(text: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // minSdk 26，通知渠道一定可用
        val channel = NotificationChannel(
            CHANNEL_ID,
            "每日学习提醒",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "提醒你完成当天的背单词任务" }
        manager.createNotificationChannel(channel)

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("该背单词了")
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }

    companion object {
        const val CHANNEL_ID = "wordbook_daily_reminder"
        const val NOTIFICATION_ID = 1001
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun settingsRepository(): SettingsRepository
    fun studyRepository(): StudyRepository
}
