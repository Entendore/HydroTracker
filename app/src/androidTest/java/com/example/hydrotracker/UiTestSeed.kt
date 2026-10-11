package com.example.hydrotracker

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hydrotracker.data.ActiveSessionStore
import com.example.hydrotracker.data.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource
import java.time.LocalDate

object UiTestSeed {
    fun resetAll() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        context.getSharedPreferences("hydration_prefs", Context.MODE_PRIVATE).edit()
            .clear()
            .putBoolean("onboarded", true)
            .putBoolean("pro_owned", true)
            .putBoolean("billing_disabled", true)
            .putString("cups_json",
                "[{\"name\":\"Cup\",\"ml\":200},{\"name\":\"Glass\",\"ml\":250}," +
                "{\"name\":\"Bottle\",\"ml\":500},{\"name\":\"Mug\",\"ml\":150}]")
            .putInt("daily_goal_ml", 2000)
            .putString("unit", "ML")
            .putString("first_launch", LocalDate.now().toString())
            .putBoolean("widget_hint_dismissed", true)
            .apply()

        try {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, "android.permission.POST_NOTIFICATIONS")
        } catch (_: Exception) { }

        runBlocking {
            AppDatabase.get(context).clearAllTables()
            ActiveSessionStore(context).clear()
            ActiveSessionStore(context).clearSummary()
        }
    }
}

class SeedRule : ExternalResource() {
    override fun before() = UiTestSeed.resetAll()
}
