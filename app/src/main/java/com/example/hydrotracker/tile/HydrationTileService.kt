package com.example.hydrotracker.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.hydrotracker.data.AppDatabase
import com.example.hydrotracker.data.WaterEntry
import com.example.hydrotracker.widget.HydrationWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class HydrationTileService : TileService() {
    override fun onStartListening() {
        qsTile?.state = Tile.STATE_ACTIVE
        qsTile?.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val pending = goAsync()
        val ctx = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(ctx).waterEntryDao()
                dao.insert(WaterEntry(amountMl = 250))
                HydrationWidgetProvider.updateAll(ctx)
                val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                val total = dao.totalSince(startOfDay)
                val goal = ctx.getSharedPreferences("hydration_prefs", MODE_PRIVATE)
                    .getInt("daily_goal_ml", 2000)
                withContext(Dispatchers.Main) {
                    qsTile?.description = "${"%,d".format(total)} / ${"%,d".format(goal)} ml today"
                    qsTile?.updateTile()
                }
            } finally { pending.finish() }
        }
    }
}
