package com.example.hydrotracker.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.hydrotracker.data.Session
import com.example.hydrotracker.data.WaterEntry
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvExporter {
    fun export(context: Context, entries: List<WaterEntry>, sessions: List<Session>): Uri? = try {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "hydrotracker_${LocalDate.now()}.csv")
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val zone = ZoneId.systemDefault()
        file.printWriter().use { pw ->
            pw.println("type,amount_ml,timestamp,datetime,details")
            entries.forEach {
                pw.println("water,${it.amountMl},${it.timestamp},${fmt.format(Instant.ofEpochMilli(it.timestamp).atZone(zone))},")
            }
            sessions.forEach {
                pw.println("session,,${it.startedAt},${fmt.format(Instant.ofEpochMilli(it.startedAt).atZone(zone))}," +
                        "\"${it.templateId};${it.intensity};${it.durationMin}min;suggested=${it.suggestedMl};drinks=${it.drinks};rpe=${it.rpe ?: "-"}\"")
            }
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) { null }
}
