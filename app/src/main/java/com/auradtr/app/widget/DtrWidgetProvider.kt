package com.auradtr.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver.PendingResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.auradtr.app.MainActivity
import com.auradtr.app.R
import com.auradtr.app.data.DtrDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class DtrWidgetProvider : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        updateAllWidgets(context, appWidgetManager, appWidgetIds, pendingResult)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.auradtr.app.ACTION_WIDGET_CLICK") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DtrWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            val pendingResult = goAsync()
            updateAllWidgets(context, appWidgetManager, appWidgetIds, pendingResult)
        }
    }

    private fun updateAllWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        pendingResult: PendingResult? = null
    ) {
        scope.launch {
            try {
                val db = DtrDatabase.getDatabase(context)
                val dao = db.dtrDao()
                val todayStr = LocalDate.now().toString()
                val activeLog = dao.getLogForDateSync(todayStr)

                // Determine status message and worked minutes
                val (statusText, workedMins, isClockedIn) = if (activeLog == null) {
                    Triple("Status: Clocked Out", 0, false)
                } else if (activeLog.clockOut != null) {
                    Triple("Status: Clocked Out", activeLog.totalWorkedMinutes, false)
                } else if (activeLog.lunchStart != null && activeLog.lunchEnd == null) {
                    // Currently on break
                    val start = Instant.parse(activeLog.clockIn)
                    val breakStart = Instant.parse(activeLog.lunchStart)
                    val duration = Duration.between(start, breakStart)
                    Triple("Status: On Break", duration.toMinutes().toInt(), false)
                } else {
                    // Clocked In / Active Duty
                    val start = Instant.parse(activeLog.clockIn)
                    var duration = Duration.between(start, Instant.now())
                    if (activeLog.lunchStart != null && activeLog.lunchEnd != null) {
                        val lunchStart = Instant.parse(activeLog.lunchStart)
                        val lunchEnd = Instant.parse(activeLog.lunchEnd)
                        duration = duration.minus(Duration.between(lunchStart, lunchEnd))
                    }
                    Triple("Status: Active Duty", duration.toMinutes().toInt(), true)
                }

                val hrs = workedMins / 60
                val mins = workedMins % 60
                val hoursText = "Today: ${hrs}h ${mins}m worked"

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.dtr_widget)
                    views.setTextViewText(R.id.widget_status_text, statusText)
                    views.setTextViewText(R.id.widget_hours_text, hoursText)

                    // Change status text coloring dynamically
                    if (isClockedIn) {
                        views.setTextColor(R.id.widget_status_text, 0xFF4ADE80.toInt()) // Light Green
                    } else {
                        views.setTextColor(R.id.widget_status_text, 0xFFFCA5A5.toInt()) // Light Red
                    }

                    // Deep-link intent to launch MainActivity
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra("deep_link_screen", "attendance")
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_button, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
