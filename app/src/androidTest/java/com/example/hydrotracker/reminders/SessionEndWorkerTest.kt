package com.example.hydrotracker.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.hydrotracker.ActiveSession
import com.example.hydrotracker.data.ActiveSessionStore
import com.example.hydrotracker.data.AppDatabase
import com.example.hydrotracker.data.Session
import com.example.hydrotracker.data.WaterEntry
import com.example.hydrotracker.data.WaterEntryDao
import com.example.hydrotracker.logic.Intensity
import com.example.hydrotracker.logic.SessionMode
import com.example.hydrotracker.logic.SipPlan
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionEndWorkerTest {

    private lateinit var context: Context
    private lateinit var store: ActiveSessionStore
    private lateinit var dao: WaterEntryDao
    private lateinit var wm: WorkManager

    companion object {
        @Volatile private var wmInitialized = false
    }

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        if (!wmInitialized) {
            WorkManagerTestInitHelper.initializeTestWorkManager(
                context,
                WorkManagerTestInitHelper.createTestInitializationBuilder()
                    .setExecutor(SynchronousExecutor()).build())
            wmInitialized = true
        }
        wm = WorkManager.getInstance(context)
        store = ActiveSessionStore(context)
        val db = AppDatabase.get(context)
        db.clearAllTables()
        dao = db.waterEntryDao()
        store.clear(); store.clearSummary()
    }

    private suspend fun seedSession(startedAt: Long, durationMin: Int = 30): ActiveSession {
        val id = dao.insertSession(
            Session(templateId = "running", intensity = "MODERATE",
                durationMin = durationMin, startedAt = startedAt, suggestedMl = 240))
        val session = ActiveSession(
            id = id, templateId = "running", intensity = Intensity.MODERATE,
            durationMin = durationMin, startedAt = startedAt,
            plan = SipPlan(everyMin = 15, count = durationMin / 15, mlPerSip = 120),
            plannedMl = 240, mode = SessionMode.STANDARD)
        store.save(session)
        return session
    }

    private suspend fun awaitFinished(id: java.util.UUID): WorkInfo = withTimeout(10_000) {
        wm.getWorkInfoByIdFlow(id).first { it != null && it.state.isFinished }!!
    }

    @Test
    fun autoEnd_finalizesSession_whenDelayElapses() = runBlocking {
        val startedAt = System.currentTimeMillis() - 60_000
        val session = seedSession(startedAt)
        dao.insert(WaterEntry(amountMl = 120, timestamp = startedAt + 30_000))

        SessionEndWorker.schedule(context, session.id, delayMin = 15)
        val enqueued = wm.getWorkInfosForUniqueWork("session_end_${session.id}").get().first()
        assertEquals(WorkInfo.State.ENQUEUED, enqueued.state)

        WorkManagerTestInitHelper.getTestDriver(context)!!
            .setInitialDelaySatisfied(enqueued.id)

        assertEquals(WorkInfo.State.SUCCEEDED, awaitFinished(enqueued.id).state)

        assertNull(store.activeSession.first())
        val summary = store.pendingSummary.first()
        assertNotNull(summary)
        assertEquals(120, summary!!.loggedMl)
        assertEquals(240, summary.plannedMl)
        assertEquals(360, summary.recoveryMl)

        val row = dao.getAllSessions().first().first { it.id == session.id }
        assertNotNull(row.endedAt)

        val pendingRecovery = wm.getWorkInfosByTag(ReminderScheduler.TAG).get()
            .filter { it.state == WorkInfo.State.ENQUEUED }
        assertEquals(3, pendingRecovery.size)
    }

    @Test
    fun autoEnd_ignoresSession_thatIsNotYetDue() = runBlocking {
        val session = seedSession(
            startedAt = System.currentTimeMillis() - 60_000, durationMin = 45)

        SessionEndWorker.schedule(context, session.id, delayMin = 0)
        val info = wm.getWorkInfosForUniqueWork("session_end_${session.id}").get().first()
        assertEquals(WorkInfo.State.SUCCEEDED, awaitFinished(info.id).state)

        assertNotNull(store.activeSession.first())
        assertNull(store.pendingSummary.first())
    }

    @Test
    fun manualEnd_cancelsPendingAutoEnd() = runBlocking {
        val session = seedSession(System.currentTimeMillis())

        SessionEndWorker.schedule(context, session.id, delayMin = 30)
        val info = wm.getWorkInfosForUniqueWork("session_end_${session.id}").get().first()

        SessionEndWorker.cancel(context, session.id)

        assertEquals(WorkInfo.State.CANCELLED, awaitFinished(info.id).state)
        assertNull(store.pendingSummary.first())
    }
}
