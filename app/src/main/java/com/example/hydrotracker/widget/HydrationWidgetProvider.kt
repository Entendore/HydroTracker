package com.example.hydrotracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.hydrotracker.MainActivity
import com.example.hydrotracker.R
import com.example.hydrotracker.data.AppDatabase
import com.example.hydrotracker.reminders.LogWaterReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

class HydrationWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch { render(context, manager, appWidgetIds) }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, HydrationWidgetProvider::class.java))
            CoroutineScope(Dispatchers.IO).launch { render(context, manager, ids) }
        }

        private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            if (ids.isEmpty()) return
            val dao = AppDatabase.get(context).waterEntryDao()
            val prefs = context.getSharedPreferences("hydration_prefs", Context.MODE_PRIVATE)

            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            val total = dao.totalSince(startOfDay)
            val today = LocalDate.now().toString()
            var goal = prefs.getInt("daily_goal_ml", 2000) +
                    if (prefs.getBoolean("active_$today", false)) 500 else 0
            goal = (goal * (if (prefs.getBoolean("hot_$today", false)) 1.25 else 1.0))
                .roundToInt().coerceAtLeast(1)

            val addIntent = PendingIntent.getBroadcast(
                context, 250,
                Intent(context, LogWaterReceiver::class.java)
                    .setAction(LogWaterReceiver.ACTION_LOG)
                    .putExtra(LogWaterReceiver.EXTRA_ML, 250),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val openIntent = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.hydration_widget).apply {
                    setTextViewText(R.id.widget_title, "${total * 100 / goal}% of goal")
                    setTextViewText(R.id.widget_text, "${"%,d".format(total)} / ${"%,d".format(goal)} ml")
                    setProgressBar(R.id.widget_progress, goal, total.coerceAtMost(goal), false)
                    setOnClickPendingIntent(R.id.widget_add, addIntent)
                    setOnClickPendingIntent(R.id.widget_open, openIntent)
                }
                manager.updateAppWidget(id, views)
            }
        }
    }
}
