package com.example.hydrotracker.reminders

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.hydrotracker.HydroApp
import com.example.hydrotracker.MainActivity
import com.example.hydrotracker.R

object NotifIds {
    /** Collision-proof: re-scheduling the same logical reminder replaces instead of stacking. */
    fun stable(purpose: String, scheduledAtMs: Long): Int =
        (purpose.hashCode() * 31 + (scheduledAtMs / 60_000L)).toInt() and 0x7FFFFFFF
}

object Notifications {
    fun show(context: Context, id: Int, title: String, text: String, logMl: Int) {
        val nm = context.getSystemService(android.app.NotificationManager::class.java) ?: return
        if (!nm.areNotificationsEnabled()) return

        val logIntent = PendingIntent.getBroadcast(
            context, id,
            Intent(context, LogWaterReceiver::class.java)
                .setAction(LogWaterReceiver.ACTION_LOG)
                .putExtra(LogWaterReceiver.EXTRA_ML, logMl),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, HydroApp.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_water)
            .setContentTitle(title).setContentText(text).setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .addAction(0, "Log +$logMl ml", logIntent)
            .build()
        try { NotificationManagerCompat.from(context).notify(id, notification) }
        catch (_: SecurityException) { }
    }
}
