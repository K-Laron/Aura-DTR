package com.auradtr.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.auradtr.app.MainActivity
import com.auradtr.app.data.DtrDatabase
import java.time.Instant
import java.time.LocalDate
import java.time.Duration

class DtrReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = DtrDatabase.getDatabase(context)
        val dao = db.dtrDao()
        
        val profile = dao.getProfileSync()
        val todayStr = LocalDate.now().toString()
        val todayLog = dao.getLogForDateSync(todayStr)

        // 1. If not clocked in, remind to clock in
        if (todayLog == null) {
            sendNotification(
                "Aura DTR Reminder",
                "You haven't clocked in for today yet. Tap here to clock in and start logging hours!",
                101
            )
            return Result.success()
        }

        // 2. If clocked in but not clocked out yet
        if (todayLog.clockOut == null) {
            val clockInInstant = Instant.parse(todayLog.clockIn)
            val workedDuration = Duration.between(clockInInstant, Instant.now())
            
            // If they have been clocked in for more than 9 hours (standard shift + lunch)
            if (workedDuration.toHours() >= 9) {
                sendNotification(
                    "Excessive Hours Alert",
                    "You've been clocked in for over 9 hours. Don't forget to submit accomplishments and clock out!",
                    102
                )
            }
        }

        return Result.success()
    }

    private fun sendNotification(title: String, message: String, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "dtr_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "DTR Attendance Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic notifications to remind interns about clocking in and out."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Premium deep teal color accent matching Aura DTR
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Using standard system alarm drawable
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFF00796B.toInt()) // Deep Teal Accent Color matching DarkPrimary/LightPrimary
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
