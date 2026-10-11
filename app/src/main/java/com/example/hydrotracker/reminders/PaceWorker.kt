package com.example.hydrotracker.reminders

import android.content.Context
import androidx.work.*
import com.example.hydrotracker.data.ActiveSessionStore
import com.example.hydrotracker.data.AppDatabase
import com.example.hydrotracker.logic.currentStreak
import com.example.hydrotracker.logic.dailyTotals
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class PaceWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val prefs = ctx.getSharedPreferences("hydration_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("pacing_enabled", true)) return Result.success()
        val nm = ctx.getSystemService(android.app.NotificationManager::class.java) ?: return Result.success()
        if (!nm.areNotificationsEnabled()) return Result.success()

        val wake = prefs.getInt("wake_min", 7 * 60)
        val sleep = prefs.getInt("sleep_min", 23 * 60)
        val now = LocalTime.now(); val nowMin = now.hour * 60 + now.minute
        if (nowMin < wake || nowMin >= sleep) return Result.success()
        if (ActiveSessionStore(ctx).activeSession.first() != null) return Result.success()

        val today = LocalDate.now().toString()
        var goal = prefs.getInt("daily_goal_ml", 2000)
        if (prefs.getBoolean("active_$today", false)) goal += 500
        if (prefs.getBoolean("hot_$today", false)) goal = (goal * 1.25).roundToInt()

        val fraction = ((nowMin - wake).toFloat() / (sleep - wake)).coerceIn(0f, 1f)
        val expected = (goal * fraction).roundToInt()

        val dao = AppDatabase.get(ctx).waterEntryDao()
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val actual = dao.totalSince(startOfDay)
        if (actual >= expected) return Result.success()

        val behind = expected - actual
        val (title, text) = if (nowMin >= 20 * 60) {
            val streak = currentStreak(dailyTotals(dao.getAllEntries().first()), goal)
            if (streak > 0) "Streak at risk 🔥" to
                    "${(goal - actual + 249) / 250} glasses left (${goal - actual} ml) to save your $streak-day streak"
            else "Behind pace 💧" to "You're ~$behind ml behind — time for a glass"
        } else "Behind pace 💧" to "You're ~$behind ml behind today's pace"

        Notifications.show(ctx, NotifIds.stable("pace", System.currentTimeMillis()), title, text, 250)
        return Result.success()
    }

    companion object {
        fun ensureScheduled(context: Context, enabled: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (enabled) wm.enqueueUniquePeriodicWork(
                "daily_pace", ExistingWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<PaceWorker>(1, TimeUnit.HOURS).build())
            else wm.cancelUniqueWork("daily_pace")
        }
    }
}
