package com.example.hydrotracker.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hydrotracker.HydrationViewModel
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(vm: HydrationViewModel) {
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    var weight by remember { mutableStateOf("") }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { vm.finishOnboarding() }

    HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
        Column(Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            when (page) {
                0 -> {
                    Text("💧", style = MaterialTheme.typography.displayLarge)
                    Text("Welcome to HydroTracker", style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center)
                    Text("Log water, follow session sip plans, and keep your streak alive — fully offline.",
                        style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(value = weight,
                        onValueChange = { weight = it.filter(Char::isDigit).take(3) },
                        label = { Text("Your weight (kg) — optional") }, singleLine = true)
                    weight.toIntOrNull()?.let { kg ->
                        Text("Suggested goal: ${kg * 33} ml",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Button(onClick = {
                            vm.setDailyGoal(kg * 33)
                            scope.launch { pager.animateScrollToPage(1) }
                        }, modifier = Modifier.padding(top = 8.dp)) { Text("Use this goal") }
                    }
                }
                1 -> {
                    Text("🥛", style = MaterialTheme.typography.displayLarge)
                    Text("Your cups", style = MaterialTheme.typography.headlineMedium)
                    Text("We start with Cup 200 · Glass 250 · Bottle 500 · Mug 150.\nRename and resize them to your real cups any time in Settings — one tap on Home logs that amount.",
                        style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { scope.launch { pager.animateScrollToPage(2) } }) { Text("Next") }
                }
                else -> {
                    Text("🔔", style = MaterialTheme.typography.displayLarge)
                    Text("Gentle reminders", style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center)
                    Text("We'll nudge you only when you're behind your daily pace — never during sleep hours, never every hour on the hour. Allow notifications to make it work.",
                        style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else vm.finishOnboarding()
                    }) { Text("Enable reminders") }
                    TextButton(onClick = { vm.finishOnboarding() }) { Text("Skip for now") }
                }
            }
            if (page < 2) {
                TextButton(onClick = { scope.launch { pager.animateScrollToPage(page + 1) } }) {
                    Text("Next")
                }
            }
        }
    }
}
