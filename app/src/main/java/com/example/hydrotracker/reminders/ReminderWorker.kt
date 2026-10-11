package com.example.hydrotracker.reminders

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    const val TAG = "hydro_reminder"

    fun schedule(context: Context, delayMin: Long, ml: Int, title: String,
                 purpose: String, scheduledAtMs: Long = System.currentTimeMillis() + delayMin * 60_000L) {
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMin, TimeUnit.MINUTES)
                .setInputData(workDataOf("ml" to ml, "title" to title,
                    "purpose" to purpose, "atMs" to scheduledAtMs))
                .addTag(TAG).build()
        )
    }

    fun cancelAll(context: Context) =
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
}

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val ml = inputData.getInt("ml", 250)
        val title = inputData.getString("title") ?: "Time for a sip 💧"
        val purpose = inputData.getString("purpose") ?: "sip"
        val atMs = inputData.getLong("atMs", System.currentTimeMillis())
        Notifications.show(applicationContext, NotifIds.stable(purpose, atMs), title,
            "Tap to log +$ml ml", ml)
        return Result.success()
    }
}
