package com.auradtr.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.auradtr.app.data.DtrDatabase
import kotlinx.coroutines.delay

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appContext = applicationContext
        val db = DtrDatabase.getDatabase(appContext)
        val dao = db.dtrDao()

        // Get all unsynced logs
        val unsyncedLogs = dao.getUnsyncedLogsSync()
        if (unsyncedLogs.isEmpty()) {
            return Result.success()
        }

        // Show a nice Android notification for sync progress
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "sync_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "DTR Synchronization",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(appContext, channelId)
            .setContentTitle("Syncing Trainee Logs")
            .setContentText("Uploading ${unsyncedLogs.size} timecards...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(unsyncedLogs.size, 0, false)

        notificationManager.notify(101, builder.build())

        // Sync logs to university server with active connectivity checks within the iteration loop
        unsyncedLogs.forEachIndexed { index, log ->
            if (!isNetworkConnected(appContext)) {
                // Connection dropped mid-sync! Stop immediately to prevent desynchronized database states
                notificationManager.cancel(101)

                val failBuilder = NotificationCompat.Builder(appContext, channelId)
                    .setContentTitle("Sync Interrupted")
                    .setContentText("Connection lost. Aura DTR will resume synchronization automatically.")
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)

                notificationManager.notify(103, failBuilder.build())
                return Result.retry()
            }

            // Simulating latency and upload of selfie attachments, accomplishments, and metadata
            delay(1500)
            
            // Mark log as synced in Room DB
            dao.markLogSynced(log.id)

            // Update progress notification
            builder.setProgress(unsyncedLogs.size, index + 1, false)
                .setContentText("Synced ${index + 1}/${unsyncedLogs.size} logs...")
            notificationManager.notify(101, builder.build())
        }

        // Completed notification
        val doneBuilder = NotificationCompat.Builder(appContext, channelId)
            .setContentTitle("Synchronization Complete")
            .setContentText("All ${unsyncedLogs.size} timecards updated securely.")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(102, doneBuilder.build())
        notificationManager.cancel(101)

        return Result.success()
    }

    /**
     * Dynamically queries active network capabilities to ensure connectivity to the internet.
     */
    private fun isNetworkConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
