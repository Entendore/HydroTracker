package com.example.hydrotracker.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.hydrotracker.data.AppDatabase
import com.example.hydrotracker.data.WaterEntry
import com.example.hydrotracker.widget.HydrationWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LogWaterReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_LOG) return
        val ml = intent.getIntExtra(EXTRA_ML, 250).coerceIn(1, 5000)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.get(context).waterEntryDao().insert(WaterEntry(amountMl = ml))
                HydrationWidgetProvider.updateAll(context)
            } finally { pending.finish() }
        }
    }
    companion object {
        const val ACTION_LOG = "com.example.hydrotracker.ACTION_LOG_WATER"
        const val EXTRA_ML = "extra_ml"
    }
}
