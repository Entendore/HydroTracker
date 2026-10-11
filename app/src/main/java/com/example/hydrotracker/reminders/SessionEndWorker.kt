package com.example.hydrotracker.reminders

import android.content.Context
import androidx.work.*
import com.example.hydrotracker.data.ActiveSessionStore
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class SessionEndWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val store = ActiveSessionStore(applicationContext)
        val session = store.activeSession.first() ?: return Result.success()

        val due = System.currentTimeMillis() - session.startedAt >= session.durationMin * 60_000L
        if (!due) return Result.success()

        val summary = SessionFinalizer.finalize(applicationContext, session)
        store.clear(); store.saveSummary(summary)

        Notifications.show(applicationContext, NotifIds.stable("session_end", session.startedAt),
            "Session complete 💪",
            if (summary.recoveryMl > 0) "Recovery plan set: ~${summary.recoveryMl} ml over the next 2 hours"
            else "Nice work — keep sipping", 250)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context, sessionId: Long, delayMin: Long) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "session_end_$sessionId", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<SessionEndWorker>()
                    .setInitialDelay(delayMin, TimeUnit.MINUTES).build()
            )
        }
        fun cancel(context: Context, sessionId: Long) =
            WorkManager.getInstance(context).cancelUniqueWork("session_end_$sessionId")
    }
}
