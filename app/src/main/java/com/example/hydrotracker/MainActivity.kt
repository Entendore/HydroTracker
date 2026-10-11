package com.example.hydrotracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.hydrotracker.billing.BillingManager
import com.example.hydrotracker.ui.*
import com.example.hydrotracker.ui.theme.HydroTrackerTheme

class MainActivity : ComponentActivity() {

    private val vm: HydrationViewModel by viewModels()
    private lateinit var billing: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        billing = BillingManager(this, onOwned = vm::setPro, onState = vm::setBillingState)
        // Test hook (seeded by instrumented tests): skips the Play connection under test
        if (!getSharedPreferences("hydration_prefs", MODE_PRIVATE)
                .getBoolean("billing_disabled", false)) billing.connect()

        setContent {
            HydroTrackerTheme {
                val onboarded by vm.onboarded.collectAsState()

                LaunchedEffect(Unit) { vm.handleDeepLink(intent) }
                val notifPerm = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()) { }
                LaunchedEffect(onboarded) {
                    if (onboarded && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                        notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                if (onboarded) {
                    val streak by vm.streak.collectAsState()
                    LaunchedEffect(streak) {
                        if (vm.shouldPromptReview()) {
                            try {
                                val mgr = com.google.android.play.core.review
                                    .ReviewManagerFactory.create(this@MainActivity)
                                mgr.requestReviewFlow().addOnCompleteListener { t ->
                                    if (t.isSuccessful) mgr.launchReviewFlow(this@MainActivity, t.result)
                                }
                            } catch (_: Exception) { }
                        }
                    }
                    HydroTrackerApp(vm, onBuyPro = { billing.purchase(this@MainActivity) })
                } else {
                    OnboardingScreen(vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.handleDeepLink(intent)
    }

    override fun onDestroy() {
        if (::billing.isInitialized) billing.destroy()
        super.onDestroy()
    }
}

private val TABS = listOf(
    "Today" to Icons.Filled.WaterDrop,
    "Session" to Icons.Filled.Timer,
    "Stats" to Icons.Filled.BarChart,
    "Settings" to Icons.Filled.Settings
)
private val TAB_TAGS = listOf("tab_today", "tab_session", "tab_stats", "tab_settings")

@Composable
fun HydroTrackerApp(vm: HydrationViewModel, onBuyPro: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val pro by vm.pro.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                TABS.forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        modifier = Modifier.testTag(TAB_TAGS[i]))
                }
            }
        }
    ) { pad ->
        when (tab) {
            0 -> HomeScreen(vm, Modifier.padding(pad), onStartSession = { tab = 1 })
            1 -> if (pro) SessionScreen(vm, Modifier.padding(pad)) else ProUpsell(vm, onBuyPro)
            2 -> StatsScreen(vm, Modifier.padding(pad))
            else -> SettingsScreen(vm, Modifier.padding(pad))
        }
    }
}

@Composable
fun ProUpsell(vm: HydrationViewModel, onBuy: () -> Unit) {
    val state by vm.billingState.collectAsState()
    Column(
        Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💪", style = MaterialTheme.typography.displayLarge)
        Text("HydroTracker Pro", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text("One-time purchase — no subscription:", style = MaterialTheme.typography.bodyLarge)
        Text("• All 14 session templates — gym, desk blocks, flights, hikes, nights out & more\n" +
                "• Sip, recovery & pace reminders\n• Sweat-rate calculator & session history\n" +
                "• CSV export\n• Offline forever, no ads, ever", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        when (state) {
            BillingManager.BillingState.UNAVAILABLE -> Text(
                "Purchases aren't available on this device (no Play Store).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error)
            else -> Button(onClick = onBuy,
                enabled = state == BillingManager.BillingState.AVAILABLE) {
                Text(if (state == BillingManager.BillingState.AVAILABLE) "Unlock Pro — $4.99"
                     else "Connecting to Play…")
            }
        }
    }
}
