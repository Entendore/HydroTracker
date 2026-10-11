package com.example.hydrotracker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hydrotracker.HydrationViewModel
import com.example.hydrotracker.data.WaterEntry
import com.example.hydrotracker.logic.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(vm: HydrationViewModel, modifier: Modifier = Modifier, onStartSession: () -> Unit) {
    val total by vm.todayTotal.collectAsState()
    val goal by vm.effectiveGoal.collectAsState()
    val unit by vm.unit.collectAsState()
    val cups by vm.cups.collectAsState()
    val streak by vm.streak.collectAsState()
    val activeDay by vm.activeDay.collectAsState()
    val hotDay by vm.hotDay.collectAsState()
    val entries by vm.allEntries.collectAsState()
    val todayEntries = entries.filter { it.isToday() }
    val context = LocalContext.current

    var showCustom by remember { mutableStateOf(false) }
    var tipIndex by rememberSaveable { mutableStateOf(tipOfTheDay()) }
    var showWidgetHint by remember { mutableStateOf(vm.widgetHintNeeded(context)) }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Hydro Tracker", Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall)
            AssistChip(onClick = {}, label = { Text("🔥 $streak") })
        }

        Spacer(Modifier.height(16.dp))

        ProgressRing(progress = if (goal > 0) total.toFloat() / goal else 0f,
            total = total, goal = goal, unit = unit,
            bonus = if (activeDay) HydrationViewModel.ACTIVE_DAY_BONUS else 0)

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = activeDay, onClick = { vm.setActiveDay(!activeDay) },
                label = { Text("🏃 Active +${HydrationViewModel.ACTIVE_DAY_BONUS}") },
                modifier = Modifier.testTag("chip_active"))
            FilterChip(selected = hotDay, onClick = { vm.setHotDay(!hotDay) },
                label = { Text("☀️ Hot day") },
                modifier = Modifier.testTag("chip_hot"))
        }

        Spacer(Modifier.height(14.dp))

        TipCard(HEALTH_TIPS[tipIndex % HEALTH_TIPS.size]) {
            tipIndex = (tipIndex + 1) % HEALTH_TIPS.size
        }

        if (showWidgetHint) {
            Spacer(Modifier.height(14.dp))
            OutlinedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📱", style = MaterialTheme.typography.headlineMedium)
                    Column(Modifier.weight(1f)) {
                        Text("Add the home-screen widget", style = MaterialTheme.typography.titleSmall)
                        Text("Long-press your launcher → Widgets → HydroTracker. Log a glass without opening the app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { vm.dismissWidgetHint(); showWidgetHint = false }) { Text("Got it") }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            cups.forEach { cup ->
                ElevatedCard(onClick = { vm.addWater(cup.ml) },
                    modifier = Modifier.width(96.dp).testTag("cup_${cup.ml}")) {
                    CardColumn(
                        icon = { Icon(Icons.Filled.WaterDrop, null,
                            tint = MaterialTheme.colorScheme.primary) },
                        title = "+${formatMl(cup.ml, unit)}", subtitle = cup.name)
                }
            }
            OutlinedCard(onClick = { showCustom = true }, modifier = Modifier.width(96.dp)) {
                CardColumn(icon = { Icon(Icons.Filled.Add, null,
                    tint = MaterialTheme.colorScheme.primary) },
                    title = "Custom", subtitle = "amount")
            }
            OutlinedCard(onClick = { vm.undoLast() }, enabled = todayEntries.isNotEmpty(),
                modifier = Modifier.width(96.dp)) {
                CardColumn(icon = { Icon(Icons.Filled.Undo, null,
                    tint = MaterialTheme.colorScheme.primary) },
                    title = "Undo", subtitle = "last entry")
            }
        }

        Spacer(Modifier.height(14.dp))

        ElevatedCard(onClick = onStartSession, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("Start a session", style = MaterialTheme.typography.titleMedium)
                    Text("Gym, desk blocks, flights, nights out — sips on schedule",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Filled.ChevronRight, null)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Today's log", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start))
        if (todayEntries.isEmpty()) {
            Text("Nothing logged yet — tap a button to add water.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp))
        } else todayEntries.forEach { entry -> EntryRow(entry) { vm.deleteEntry(entry) } }
    }

    if (showCustom) {
        NumberDialog(
            title = "Custom amount",
            label = "Amount (${if (unit == VolumeUnit.ML) "ml" else "fl oz"})",
            initialText = "",
            transform = { toMl(it.toDouble(), unit) },
            onConfirm = { vm.addWater(it); showCustom = false },
            onDismiss = { showCustom = false })
    }
}

@Composable
private fun ProgressRing(progress: Float, total: Int, goal: Int, unit: VolumeUnit, bonus: Int) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(targetValue = target)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val ring = MaterialTheme.colorScheme.primary
    val percent = (target * 100).roundToInt()

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(230.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 22.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, -90f, 360f, false, topLeft = topLeft, size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round))
            if (animated > 0.01f) drawArc(ring, -90f, 360f * animated, false,
                topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("%,d".format(total),
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 44.sp),
                fontWeight = FontWeight.Bold, maxLines = 1,
                modifier = Modifier.testTag("total_ml"))
            Text("of ${formatMl(goal, unit)} goal",
                style = MaterialTheme.typography.bodyLarge, maxLines = 1,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("goal_line"))
            Text("$percent% of goal", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            if (bonus > 0) Text("+$bonus ml — active day",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun CardColumn(icon: @Composable () -> Unit, title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        icon()
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EntryRow(entry: WaterEntry, onDelete: () -> Unit) {
    val time = remember(entry.timestamp) {
        DateTimeFormatter.ofPattern("h:mm a")
            .format(Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()))
    }
    ListItem(
        headlineContent = { Text("+${entry.amountMl} ml") },
        supportingContent = { Text(time) },
        leadingContent = { Icon(Icons.Filled.WaterDrop, null,
            tint = MaterialTheme.colorScheme.primary) },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
            }
        })
}
