package com.example.hydrotracker.ui

import android.app.NotificationManagerCompat
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.hydrotracker.BuildConfig
import com.example.hydrotracker.HydrationViewModel
import com.example.hydrotracker.logic.CupPreset
import com.example.hydrotracker.logic.TIP_SOURCES
import com.example.hydrotracker.logic.VolumeUnit
import com.example.hydrotracker.logic.formatMl
import com.example.hydrotracker.logic.toMl

@Composable
fun SettingsScreen(vm: HydrationViewModel, modifier: Modifier = Modifier) {
    val goal by vm.dailyGoal.collectAsState()
    val unit by vm.unit.collectAsState()
    val cups by vm.cups.collectAsState()
    val sweatRate by vm.sweatRate.collectAsState()
    val pacingEnabled by vm.pacingEnabled.collectAsState()
    val wakeMinutes by vm.wakeMinutes.collectAsState()
    val sleepMinutes by vm.sleepMinutes.collectAsState()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hydration_prefs", Context.MODE_PRIVATE) }

    var showGoalDialog by remember { mutableStateOf(false) }
    var showSources by remember { mutableStateOf(false) }
    var showBattery by remember { mutableStateOf(!prefs.getBoolean("dismissed_battery_card", false)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var notifsOn by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME)
                notifsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        if (!notifsOn) {
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Reminders are off", style = MaterialTheme.typography.titleMedium)
                    Text("Workout sips, recovery and pace reminders won't appear.",
                        style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    }, modifier = Modifier.padding(top = 8.dp)) { Text("Enable notifications") }
                }
            }
        }

        OutlinedCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Daily goal", style = MaterialTheme.typography.titleMedium)
                    Text(formatMl(goal, unit), color = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = { showGoalDialog = true }) { Text("Edit") }
            }
        }

        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Units", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    VolumeUnit.entries.forEachIndexed { i, u ->
                        SegmentedButton(selected = unit == u, onClick = { vm.setUnit(u) },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = 2)
                        ) { Text(if (u == VolumeUnit.ML) "ml" else "fl oz") }
                    }
                }
            }
        }

        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Your cups", style = MaterialTheme.typography.titleMedium)
                cups.forEachIndexed { index, cup ->
                    CupRow(index, cup,
                        onChange = { i, c -> vm.updateCups(cups.mapIndexed { j, old -> if (j == i) c else old }) },
                        onDelete = { i -> vm.updateCups(cups.filterIndexed { j, _ -> j != i }) })
                }
                if (cups.size < 6) {
                    TextButton(onClick = { vm.updateCups(cups + CupPreset("New cup", 250)) }) {
                        Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Add cup")
                    }
                }
            }
        }

        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Daily reminders", style = MaterialTheme.typography.titleMedium)
                        Text("Nudges only when you fall behind your pace",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = pacingEnabled, onCheckedChange = { vm.setPacingEnabled(it) })
                }
                if (pacingEnabled) {
                    TimeRow("Wake time", wakeMinutes) { vm.setWakeMinutes(it) }
                    TimeRow("Sleep time", sleepMinutes) { vm.setSleepMinutes(it) }
                    Text("Crossing time zones won't break your streak (travel grace).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (showBattery) {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Best results: unrestricted battery",
                        style = MaterialTheme.typography.titleMedium)
                    Text("Aggressive battery savers delay reminders. Set HydroTracker to \u201cUnrestricted\u201d in system settings. (Samsung: Battery → Unlimited · Xiaomi: Autostart + No restrictions · Huawei: Launch → Manage manually.)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(android.net.Uri.fromParts("package", context.packageName, null)))
                        }) { Text("Open settings") }
                        TextButton(onClick = {
                            prefs.edit().putBoolean("dismissed_battery_card", true).apply()
                            showBattery = false
                        }) { Text("Dismiss") }
                    }
                }
            }
        }

        OutlinedCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Measured sweat rate", style = MaterialTheme.typography.titleMedium)
                Text(if (sweatRate > 0) "%.2f L per hour".format(sweatRate)
                     else "Not measured — see the Session tab",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("About", style = MaterialTheme.typography.titleMedium)
                Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
                Text("All data stays on this device. Nothing is transmitted, ever.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Health tips are educational information only — not medical advice.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Built with Jetpack Compose, Room, WorkManager, DataStore, Billing — their licenses apply.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { showSources = !showSources }) {
                    Text(if (showSources) "Hide sources" else "Tip sources & bibliography")
                }
                if (showSources) {
                    TIP_SOURCES.values.flatten().distinctBy { it.url }
                        .sortedWith(compareBy({ it.kind }, { it.title }))
                        .forEach { SourceRow(it) }
                }
            }
        }

        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Data", style = MaterialTheme.typography.titleMedium)
                Button(onClick = {
                    vm.exportCsv { uri ->
                        if (uri == null) Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                        else {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(send, "Share export"))
                        }
                    }
                }) { Text("Export CSV") }
                Text("All water entries and sessions, shared via your favorite app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showGoalDialog) {
        val initial = if (unit == VolumeUnit.ML) goal.toString()
                      else (goal / 29.5735).toInt().toString()
        NumberDialog(
            title = "Daily goal",
            label = "Goal (${if (unit == VolumeUnit.ML) "ml" else "fl oz"})",
            initialText = initial,
            transform = { toMl(it.toDouble(), unit) },
            onConfirm = { vm.setDailyGoal(it); showGoalDialog = false },
            onDismiss = { showGoalDialog = false })
    }
}

@Composable
private fun TimeRow(label: String, minutes: Int, onChange: (Int) -> Unit) {
    Column {
        Text("$label  ·  %02d:%02d".format(minutes / 60, minutes % 60),
            style = MaterialTheme.typography.bodyMedium)
        Slider(value = minutes.toFloat(),
            onValueChange = { onChange(((it.toInt() / 15) * 15)) },
            valueRange = 0f..1425f, steps = 94)
    }
}

@Composable
private fun CupRow(index: Int, cup: CupPreset,
                    onChange: (Int, CupPreset) -> Unit, onDelete: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = cup.name,
            onValueChange = { onChange(index, cup.copy(name = it)) },
            modifier = Modifier.weight(1f), singleLine = true)
        OutlinedTextField(value = cup.ml.toString(),
            onValueChange = { s -> s.toIntOrNull()?.let { onChange(index, cup.copy(ml = it)) } },
            modifier = Modifier.width(90.dp), label = { Text("ml") }, singleLine = true)
        IconButton(onClick = { onDelete(index) }) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove cup")
        }
    }
}
