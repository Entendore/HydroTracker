package com.example.hydrotracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hydrotracker.ActiveSession
import com.example.hydrotracker.HydrationViewModel
import com.example.hydrotracker.SessionSummary
import com.example.hydrotracker.data.Session
import com.example.hydrotracker.data.WaterEntry
import com.example.hydrotracker.logic.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun SessionScreen(vm: HydrationViewModel, modifier: Modifier = Modifier) {
    val session by vm.session.collectAsState()
    val summary by vm.lastSummary.collectAsState()
    val sessions by vm.sessions.collectAsState()
    val entries by vm.allEntries.collectAsState()
    val sweatRate by vm.sweatRate.collectAsState()

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Session", style = MaterialTheme.typography.headlineSmall)
        session?.let { ActiveSessionCard(it, vm, entries) }
        summary?.let { sm ->
            SessionSummaryCard(sm, onDismiss = vm::clearSummary,
                onLogElectrolytes = { vm.addWater(500); vm.clearSummary() },
                onRate = { vm.rateLast(it); vm.clearSummary() })
        }
        if (session == null) NewSessionCard(vm, sweatRate)
        RecentSessions(sessions.take(8), entries)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun NewSessionCard(vm: HydrationViewModel, sweatRate: Double) {
    val hotDay by vm.hotDay.collectAsState()
    var category by remember { mutableStateOf(TemplateCategory.EXERCISE) }
    var templateId by remember { mutableStateOf("running") }
    var intensity by remember { mutableStateOf(Intensity.MODERATE) }
    var minutes by remember { mutableStateOf(30) }
    var showSweat by remember { mutableStateOf(false) }

    val t = template(templateId)
    LaunchedEffect(templateId) {
        minutes = t.durationPresets.firstOrNull() ?: ((t.minMin + t.maxMin) / 2)
    }

    val planned = suggestedMl(t, intensity, minutes, sweatRate.takeIf { it > 0 })
    val (plannedAdj, everyAdj) =
        if (hotDay) applyHotDay(planned, t.sipEveryMin) else planned to t.sipEveryMin
    val plan = if (t.mode == SessionMode.NIGHT_OUT) SipPlan(everyAdj, 0, DRINK_MATCH_ML)
               else sipPlan(minutes, plannedAdj, everyAdj)

    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("New session", style = MaterialTheme.typography.titleMedium)

            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TemplateCategory.entries.forEach { c ->
                    FilterChip(selected = c == category, onClick = {
                        category = c
                        templateId = SESSION_TEMPLATES.first { it.category == c }.id
                    }, label = { Text(c.label) })
                }
            }

            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SESSION_TEMPLATES.filter { it.category == category }.forEach { p ->
                    FilterChip(selected = p.id == templateId, onClick = { templateId = p.id },
                        label = { Text(p.label) },
                        leadingIcon = { Icon(p.icon, null, Modifier.size(18.dp)) })
                }
            }

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Intensity.entries.forEachIndexed { i, it ->
                    SegmentedButton(selected = intensity == it, onClick = { intensity = it },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = Intensity.entries.size)
                    ) { Text(it.label) }
                }
            }

            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                t.durationPresets.forEach { d ->
                    FilterChip(selected = minutes == d, onClick = { minutes = d },
                        label = { Text(if (d >= 60) "${d / 60}h" + (if (d % 60 != 0) " ${d % 60}m" else "") else "${d}m") })
                }
            }
            Text("${minutes} min", style = MaterialTheme.typography.titleMedium)
            Slider(value = minutes.toFloat(), onValueChange = { minutes = it.roundToInt() },
                valueRange = t.minMin.toFloat()..t.maxMin.toFloat(),
                steps = ((t.maxMin - t.minMin) / 5 - 1).coerceAtLeast(0))

            if (t.preMl > 0) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t.preAdvice ?: "", Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall)
                    FilledTonalButton(onClick = { vm.addWater(t.preMl) }) { Text("Log +${t.preMl}") }
                }
            }

            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (t.mode == SessionMode.NIGHT_OUT) {
                        Text("1-for-1: ${DRINK_MATCH_ML} ml of water per drink",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Tap the beer button each round — we'll remind you to match it.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    } else {
                        Text("Drink ≈ $plannedAdj ml during this session",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${plan.mlPerSip} ml every ${plan.everyMin} min (${plan.count} sips)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    if (hotDay) Text("☀️ Hot day: +20% target, sips every $everyAdj min",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            TextButton(onClick = { showSweat = !showSweat }) {
                Text(if (showSweat) "Hide sweat-rate calculator" else "Sweat-rate calculator 🧪")
            }
            if (showSweat) SweatCalculator(vm)

            Button(onClick = { vm.startSession(templateId, intensity, minutes) },
                modifier = Modifier.fillMaxWidth().testTag("start_session")) { Text("Start session") }
        }
    }
}

@Composable
private fun SweatCalculator(vm: HydrationViewModel) {
    var before by remember { mutableStateOf("70.0") }
    var after by remember { mutableStateOf("69.4") }
    var fluid by remember { mutableStateOf("500") }
    var mins by remember { mutableStateOf("45") }

    val b = before.toDoubleOrNull(); val a = after.toDoubleOrNull()
    val f = fluid.toIntOrNull(); val m = mins.toIntOrNull()
    val rate = if (b != null && a != null && f != null && m != null && m > 0)
        sweatRateLph(b - a, f, m) else null

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(before, { before = it }, Modifier.weight(1f),
                label = { Text("Weight before (kg)") }, singleLine = true)
            OutlinedTextField(after, { after = it }, Modifier.weight(1f),
                label = { Text("Weight after (kg)") }, singleLine = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(fluid, { fluid = it }, Modifier.weight(1f),
                label = { Text("Fluid during (ml)") }, singleLine = true)
            OutlinedTextField(mins, { mins = it }, Modifier.weight(1f),
                label = { Text("Duration (min)") }, singleLine = true)
        }
        if (rate != null && rate > 0) Text("Sweat rate ≈ %.2f L per hour".format(rate),
            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text("Losing more than 2% of body weight during exercise is a sign you're under-drinking.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(enabled = rate != null && rate > 0,
            onClick = { rate?.let { vm.saveSweatRate(it) } }) { Text("Use this for suggestions") }
    }
}

@Composable
private fun ActiveSessionCard(s: ActiveSession, vm: HydrationViewModel, entries: List<WaterEntry>) {
    var nowMs by remember(s.id) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(s.id) { while (true) { delay(1_000); nowMs = System.currentTimeMillis() } }
    val elapsedMs = (nowMs - s.startedAt).coerceAtLeast(0)
    val elapsedMin = elapsedMs / 60_000.0
    LaunchedEffect(elapsedMin) { if (elapsedMin >= s.durationMin) vm.endSession() }

    val t = template(s.templateId)
    val timeFrac = (elapsedMin / s.durationMin).toFloat().coerceIn(0f, 1f)
    val logged = entries.filter { it.timestamp >= s.startedAt }.sumOf { it.amountMl }

    Card(colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${t.label} in progress", style = MaterialTheme.typography.titleMedium)
            Text("%02d:%02d / %02d:00".format(
                elapsedMin.toInt(), (elapsedMs / 1000 % 60).toInt(), s.durationMin),
                style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            LinearProgressIndicator(progress = { timeFrac }, modifier = Modifier.fillMaxWidth())

            if (s.mode == SessionMode.STANDARD) {
                val sipsDue = (elapsedMin / s.plan.everyMin).toInt().coerceAtMost(s.plan.count)
                Text("Sips: $sipsDue/${s.plan.count} · Logged: $logged / ${s.plannedMl} ml",
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                PaceBar(if (s.plannedMl > 0) logged.toFloat() / s.plannedMl else 0f, timeFrac)
                PaceText(logged, (s.plannedMl * elapsedMin / s.durationMin).roundToInt(),
                    MaterialTheme.colorScheme.onPrimaryContainer)
                Button(onClick = { vm.logSip() },
                    modifier = Modifier.fillMaxWidth().testTag("log_sip")) {
                    Text("Log sip +${s.plan.mlPerSip} ml") }
            } else {
                val expected = s.drinks * DRINK_MATCH_ML
                Text("Drinks: ${s.drinks} · Match so far: $logged / $expected ml",
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                PaceBar(if (expected > 0) logged.toFloat() / expected else 0f, timeFrac)
                PaceText(logged, expected, MaterialTheme.colorScheme.onPrimaryContainer)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.logDrink() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.SportsBar, null); Spacer(Modifier.width(4.dp))
                        Text("Had a drink")
                    }
                    FilledTonalButton(onClick = { vm.addWater(DRINK_MATCH_ML) },
                        modifier = Modifier.weight(1f)) { Text("Water +$DRINK_MATCH_ML") }
                }
            }

            OutlinedButton(onClick = { vm.endSession() },
                modifier = Modifier.fillMaxWidth().testTag("end_session")) { Text("End session") }
        }
    }
}

@Composable
private fun PaceBar(waterFraction: Float, timeFraction: Float) {
    Box {
        LinearProgressIndicator(progress = { waterFraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(10.dp))
        Box(Modifier.fillMaxWidth(timeFraction.coerceIn(0f, 1f)).height(14.dp),
            contentAlignment = Alignment.CenterEnd) {
            Box(Modifier.width(3.dp).height(14.dp).background(MaterialTheme.colorScheme.onSurface))
        }
    }
}

@Composable
private fun PaceText(logged: Int, expected: Int, color: androidx.compose.ui.graphics.Color) {
    val diff = logged - expected
    Text(when {
        expected == 0 -> "Log as you go — the marker shows your pace."
        diff >= 0 -> "On pace 💪 ($diff ml ahead)"
        else -> "Behind pace — log ${-diff} ml to catch up"
    }, style = MaterialTheme.typography.labelMedium, color = color)
}

@Composable
private fun SessionSummaryCard(summary: SessionSummary, onDismiss: () -> Unit,
                               onLogElectrolytes: () -> Unit, onRate: (Int) -> Unit) {
    Card(colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Session complete 🎉", style = MaterialTheme.typography.titleMedium)
            Text("You drank ${summary.loggedMl} ml of the planned ${summary.plannedMl} ml.")
            if (summary.recoveryMl > 0) Text(
                "Recovery plan: ~${summary.recoveryMl} ml over the next 2 hours — reminders are set.",
                style = MaterialTheme.typography.bodyMedium)
            if (summary.electrolytes) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Bolt, null)
                    Text("You likely sweated 700+ ml. Top up sodium & potassium — sports drink, coconut water, or a salty snack + water.")
                }
                Button(onClick = onLogElectrolytes) { Text("Log 500 ml electrolyte drink") }
            }
            Text("How hard was it? (1–10)", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..10).forEach { n ->
                    FilterChip(selected = false, onClick = { onRate(n) }, label = { Text("$n") })
                }
            }
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
private fun RecentSessions(list: List<Session>, entries: List<WaterEntry>) {
    Text("Recent sessions", style = MaterialTheme.typography.titleMedium)
    if (list.isEmpty()) {
        Text("No sessions yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    list.forEach { s ->
        val t = SESSION_TEMPLATES.firstOrNull { it.id == s.templateId }
        val logged = s.endedAt?.let { end ->
            entries.filter { it.timestamp in s.startedAt..end }.sumOf { it.amountMl }
        } ?: 0
        ListItem(
            headlineContent = { Text(
                "${t?.label ?: s.templateId} · ${s.intensity.lowercase().replaceFirstChar { it.uppercase() }}") },
            supportingContent = { Text(
                "${s.durationMin} min · planned ${s.suggestedMl} ml · logged $logged ml" +
                        (if (s.drinks > 0) " · ${s.drinks} drinks" else "") +
                        (s.rpe?.let { " · effort $it/10" } ?: "")) },
            leadingContent = { Icon(t?.icon ?: Icons.Filled.Timer, null,
                tint = MaterialTheme.colorScheme.primary) })
    }
}
