package com.example.hydrotracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class HydroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_REMINDERS, "Hydration reminders", NotificationManager.IMPORTANCE_DEFAULT
        ).description = "Sip, recovery and pace reminders"
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
    companion object { const val CHANNEL_REMINDERS = "reminders" }
}
