package com.example.hydrotracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hydrotracker.HydrationViewModel
import com.example.hydrotracker.data.WaterEntry
import com.example.hydrotracker.logic.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StatsScreen(vm: HydrationViewModel, modifier: Modifier = Modifier) {
    val entries by vm.allEntries.collectAsState()
    val goal by vm.dailyGoal.collectAsState()
    val unit by vm.unit.collectAsState()
    val streak by vm.streak.collectAsState()
    val bestStreak by vm.bestStreak.collectAsState()
    val badges by vm.badges.collectAsState()
    val insights by vm.insights.collectAsState()
    val days = remember(entries) {
        dailyTotals(entries).toSortedMap(compareByDescending<LocalDate> { it })
    }

    LazyColumn(modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {

        item { Text("Stats", style = MaterialTheme.typography.headlineSmall) }

        item {
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.LocalFireDepartment, null, tint = Color(0xFFFF7043),
                        modifier = Modifier.size(40.dp))
                    Column {
                        Text("$streak-day streak", style = MaterialTheme.typography.headlineSmall)
                        Text("Best ever: $bestStreak days · current goal ${formatMl(goal, unit)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Text("Badges", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(badges) { BadgeChip(it) }
            }
        }

        item { TrendsSection(entries, goal, unit) }

        item {
            Text("Insights", style = MaterialTheme.typography.titleMedium)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    insights.forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }

        item {
            var tipIndex by remember { mutableStateOf(tipOfTheDay()) }
            Text("Health tips", style = MaterialTheme.typography.titleMedium)
            Text("Educational information only — not medical advice.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            TipCard(HEALTH_TIPS[tipIndex % HEALTH_TIPS.size]) { tipIndex++ }
            Spacer(Modifier.height(10.dp))

            val hotDay by vm.hotDay.collectAsState()
            val activeDay by vm.activeDay.collectAsState()
            val session by vm.session.collectAsState()
            val contextual = remember(hotDay, activeDay, session?.templateId) {
                contextualTips(hotDay, activeDay, LocalTime.now().hour, session?.templateId)
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Relevant right now", style = MaterialTheme.typography.labelLarge)
                    contextual.forEach { tip ->
                        Column {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(tip.emoji)
                                Column {
                                    Text(tip.title, style = MaterialTheme.typography.labelLarge)
                                    Text(tip.text, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            SourceLinks(tip, compact = true)
                        }
                    }
                }
            }
        }

        item { Text("History", style = MaterialTheme.typography.titleMedium) }
        if (days.isEmpty()) {
            item { Text("No entries yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        days.forEach { (day, totalMl) ->
            item(key = day.toEpochDay()) { DayCard(day, totalMl, goal) }
        }
    }
}

@Composable
private fun BadgeChip(badge: Badge) {
    Card(colors = CardDefaults.cardColors(
        containerColor = if (badge.earned) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(10.dp).width(92.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(badge.emoji, style = MaterialTheme.typography.headlineMedium)
            Text(badge.title, style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TrendsSection(entries: List<WaterEntry>, goal: Int, unit: VolumeUnit) {
    var span by remember { mutableStateOf(7) }
    val totals = remember(entries) { dailyTotals(entries) }
    val today = LocalDate.now()
    val data = remember(span, totals) {
        (0 until span).map { off ->
            val d = today.minusDays((span - 1 - off).toLong())
            d to (totals[d] ?: 0)
        }
    }
    val avg = data.map { it.second }.average().roundToInt()
    val hit = data.count { it.second >= goal }

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Trends", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(7, 30).forEach { s ->
                    FilterChip(selected = span == s, onClick = { span = s }, label = { Text("${s}d") })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        TrendsChart(data, goal, showLabels = span == 7)
        Spacer(Modifier.height(6.dp))
        Text("Average ${formatMl(avg, unit)} · goal hit on $hit of $span days",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TrendsChart(data: List<Pair<LocalDate, Int>>, goal: Int, showLabels: Boolean) {
    val barColor = MaterialTheme.colorScheme.primary
    val lineColor = MaterialTheme.colorScheme.tertiary
    val maxVal = maxOf(goal, data.maxOfOrNull { it.second } ?: 0) * 1.15f

    Column {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val n = data.size.coerceAtLeast(1)
            val slot = size.width / n
            val barWidth = slot * 0.65f
            val goalY = size.height * (1f - goal / maxVal)
            drawLine(lineColor, Offset(0f, goalY), Offset(size.width, goalY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
            data.forEachIndexed { i, (_, v) ->
                val h = size.height * (v / maxVal).coerceIn(0f, 1f)
                drawRoundRect(barColor,
                    topLeft = Offset(i * slot + (slot - barWidth) / 2, size.height - h),
                    size = Size(barWidth, h), cornerRadius = CornerRadius(4.dp.toPx()))
            }
        }
        if (showLabels) {
            Row(Modifier.fillMaxWidth()) {
                data.forEach { (day, _) ->
                    Text(day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DayCard(day: LocalDate, totalMl: Int, goalMl: Int) {
    val progress = if (goalMl > 0) (totalMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f
    val goalMet = goalMl > 0 && totalMl >= goalMl
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(dayLabel(day), style = MaterialTheme.typography.titleMedium)
                    Text("$totalMl / $goalMl ml", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (goalMet) Icon(Icons.Filled.CheckCircle, contentDescription = "Goal met",
                    tint = Color(0xFF4CAF50))
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun dayLabel(day: LocalDate): String = when (day) {
    LocalDate.now() -> "Today"
    LocalDate.now().minusDays(1) -> "Yesterday"
    else -> day.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
}
