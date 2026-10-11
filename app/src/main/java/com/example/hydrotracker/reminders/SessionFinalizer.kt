package com.example.hydrotracker.reminders

import android.content.Context
import com.example.hydrotracker.ActiveSession
import com.example.hydrotracker.SessionSummary
import com.example.hydrotracker.data.AppDatabase
import com.example.hydrotracker.logic.*
import kotlin.math.roundToInt

/** Shared by the ViewModel (manual end) and SessionEndWorker (auto end) — one implementation. */
object SessionFinalizer {
    suspend fun finalize(context: Context, session: ActiveSession): SessionSummary {
        val dao = AppDatabase.get(context).waterEntryDao()
        val prefs = context.getSharedPreferences("hydration_prefs", Context.MODE_PRIVATE)
        val endedAt = System.currentTimeMillis()

        ReminderScheduler.cancelAll(context)
        val actualMin = ((endedAt - session.startedAt) / 60_000.0)
            .roundToInt().coerceIn(1, session.durationMin)
        dao.endSession(session.id, endedAt, actualMin)

        val t = template(session.templateId)
        val rate = prefs.getFloat("sweat_rate_lph", 0f).toDouble().takeIf { it > 0 }
        val sweatEst = rate?.let { (it * 1000 * actualMin / 60.0).roundToInt() } ?: session.plannedMl

        var recoveryMl = 0
        if (session.mode == SessionMode.STANDARD && t.recoveryPlan) {
            val rp = recoveryPlan(sweatEst)
            rp.reminders.forEach { (delayMin, ml) ->
                ReminderScheduler.schedule(context, delayMin, ml, "Recovery: rehydrate 💧",
                    "recovery", endedAt + delayMin * 60_000L)
            }
            recoveryMl = rp.totalMl
        }

        val logged = dao.totalSince(session.startedAt)
        return SessionSummary(
            templateId = session.templateId, plannedMl = session.plannedMl, loggedMl = logged,
            electrolytes = needsElectrolytes(t, session.intensity, actualMin, rate),
            recoveryMl = recoveryMl, mode = session.mode
        )
    }
}
