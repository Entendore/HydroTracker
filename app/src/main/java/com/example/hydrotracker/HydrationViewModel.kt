package com.example.hydrotracker

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hydrotracker.billing.BillingManager
import com.example.hydrotracker.data.ActiveSessionStore
import com.example.hydrotracker.data.AppDatabase
import com.example.hydrotracker.data.Session
import com.example.hydrotracker.data.WaterEntry
import com.example.hydrotracker.export.CsvExporter
import com.example.hydrotracker.logic.*
import com.example.hydrotracker.reminders.PaceWorker
import com.example.hydrotracker.reminders.ReminderScheduler
import com.example.hydrotracker.reminders.SessionEndWorker
import com.example.hydrotracker.reminders.SessionFinalizer
import com.example.hydrotracker.widget.HydrationWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

data class ActiveSession(
    val id: Long, val templateId: String, val intensity: Intensity, val durationMin: Int,
    val startedAt: Long, val plan: SipPlan, val plannedMl: Int, val mode: SessionMode,
    val drinks: Int = 0
)

data class SessionSummary(
    val templateId: String, val plannedMl: Int, val loggedMl: Int,
    val electrolytes: Boolean, val recoveryMl: Int, val mode: SessionMode
)

data class CupPreset(val name: String, val ml: Int)

val DEFAULT_CUPS = listOf(
    CupPreset("Cup", 200), CupPreset("Glass", 250),
    CupPreset("Bottle", 500), CupPreset("Mug", 150)
)

fun cupsFromJson(json: String?): List<CupPreset> = try {
    val arr = org.json.JSONArray(json)
    (0 until arr.length()).map { i ->
        arr.getJSONObject(i).let { CupPreset(it.getString("name"), it.getInt("ml")) }
    }
} catch (e: Exception) { DEFAULT_CUPS }

fun cupsToJson(cups: List<CupPreset>): String = org.json.JSONArray().apply {
    cups.forEach { put(org.json.JSONObject().put("name", it.name).put("ml", it.ml)) }
}.toString()

class HydrationViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext = app
    private val dao = AppDatabase.get(app).waterEntryDao()
    private val prefs = app.getSharedPreferences("hydration_prefs", Application.MODE_PRIVATE)
    private val sessionStore = ActiveSessionStore(app)

    init {
        cleanupStalePrefs()
        detectTimezoneChange()
        if (!prefs.contains(KEY_FIRST_LAUNCH)) {
            prefs.edit().putString(KEY_FIRST_LAUNCH, LocalDate.now().toString()).apply()
        }
        PaceWorker.ensureScheduled(appContext, prefs.getBoolean(KEY_PACING, true))

        viewModelScope.launch {
            sessionStore.pendingSummary.first()?.let { summary ->
                _lastSummary.value = summary
                sessionStore.clearSummary()
                return@launch
            }
            val restored = sessionStore.activeSession.first() ?: return@launch
            if (System.currentTimeMillis() - restored.startedAt >= restored.durationMin * 60_000L) {
                _session.value = restored
                endSession()
            } else _session.value = restored
        }
    }

    private fun cleanupStalePrefs() {
        val cutoff = LocalDate.now().minusDays(1).toString()
        val stale = prefs.all.keys.filter {
            (it.startsWith("active_") || it.startsWith("hot_")) && it.substringAfter('_') < cutoff
        }
        if (stale.isNotEmpty()) prefs.edit().apply { stale.forEach(::remove) }.apply()
    }

    // ---------- Settings ----------
    private val _dailyGoal = MutableStateFlow(prefs.getInt(KEY_GOAL, DEFAULT_GOAL))
    val dailyGoal = _dailyGoal.asStateFlow()

    private val _unit = MutableStateFlow(
        if (prefs.getString(KEY_UNIT, "ML") == "OZ") VolumeUnit.OZ else VolumeUnit.ML)
    val unit = _unit.asStateFlow()

    private val _cups = MutableStateFlow(prefs.getString(KEY_CUPS, null)?.let(::cupsFromJson) ?: DEFAULT_CUPS)
    val cups = _cups.asStateFlow()

    private val _sweatRate = MutableStateFlow(prefs.getFloat(KEY_SWEAT, 0f).toDouble())
    val sweatRate = _sweatRate.asStateFlow()

    // ---------- Active day / hot day ----------
    private fun activeDayKey() = "active_${LocalDate.now()}"
    private fun hotDayKey() = "hot_${LocalDate.now()}"

    private val _activeDay = MutableStateFlow(prefs.getBoolean(activeDayKey(), false))
    val activeDay = _activeDay.asStateFlow()

    private val _hotDay = MutableStateFlow(prefs.getBoolean(hotDayKey(), false))
    val hotDay = _hotDay.asStateFlow()

    fun setActiveDay(on: Boolean) {
        prefs.edit().putBoolean(activeDayKey(), on).apply()
        _activeDay.value = on
        HydrationWidgetProvider.updateAll(appContext)
    }

    fun setHotDay(on: Boolean) {
        prefs.edit().putBoolean(hotDayKey(), on).apply()
        _hotDay.value = on
        HydrationWidgetProvider.updateAll(appContext)
    }

    val effectiveGoal = combine(_dailyGoal, _activeDay, _hotDay) { g, active, hot ->
        ((g + if (active) ACTIVE_DAY_BONUS else 0) * (if (hot) HOT_DAY_MULTIPLIER else 1.0)).roundToInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), _dailyGoal.value)

    // ---------- Data ----------
    val allEntries: StateFlow<List<WaterEntry>> = dao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sessions: StateFlow<List<Session>> = dao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayTotal: StateFlow<Int> = allEntries
        .map { list -> list.filter { it.isToday() }.sumOf { it.amountMl } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ---------- Stats ----------
    val streak: StateFlow<Int> =
        combine(allEntries, _dailyGoal) { e, g -> currentStreak(dailyTotals(e), g, graceDays()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val bestStreak: StateFlow<Int> =
        combine(allEntries, _dailyGoal) { e, g -> bestStreakEver(dailyTotals(e), g) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val badges: StateFlow<List<Badge>> =
        combine(allEntries, _dailyGoal) { e, g -> buildBadges(e, g) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val insights: StateFlow<List<String>> =
        combine(allEntries, _dailyGoal) { e, g -> buildInsights(e, g) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---------- Billing ----------
    private val _pro = MutableStateFlow(prefs.getBoolean("pro_owned", false))
    val pro = _pro.asStateFlow()
    fun setPro(owned: Boolean) { _pro.value = owned }

    val billingState = MutableStateFlow(BillingManager.BillingState.CONNECTING)
    fun setBillingState(state: BillingManager.BillingState) { billingState.value = state }

    // ---------- Daily pacing ----------
    val pacingEnabled = MutableStateFlow(prefs.getBoolean(KEY_PACING, true))
    val wakeMinutes = MutableStateFlow(prefs.getInt("wake_min", 7 * 60))
    val sleepMinutes = MutableStateFlow(prefs.getInt("sleep_min", 23 * 60))

    fun setPacingEnabled(on: Boolean) {
        prefs.edit().putBoolean(KEY_PACING, on).apply()
        pacingEnabled.value = on
        PaceWorker.ensureScheduled(appContext, on)
    }
    fun setWakeMinutes(min: Int) { prefs.edit().putInt("wake_min", min).apply(); wakeMinutes.value = min }
    fun setSleepMinutes(min: Int) { prefs.edit().putInt("sleep_min", min).apply(); sleepMinutes.value = min }

    // ---------- Timezone travel grace ----------
    private fun detectTimezoneChange() {
        val tz = java.time.ZoneId.systemDefault().id
        if (prefs.getString(KEY_TZ, null) != tz) {
            val changes = (prefs.getStringSet(KEY_TZ_CHANGES, emptySet()).orEmpty() +
                    LocalDate.now().toEpochDay().toString()).sortedDescending().take(10).toSet()
            prefs.edit().putString(KEY_TZ, tz).putStringSet(KEY_TZ_CHANGES, changes).apply()
        }
    }
    private fun graceDays(): Set<Long> =
        prefs.getStringSet(KEY_TZ_CHANGES, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()

    // ---------- Onboarding / hints / review ----------
    private val _onboarded = MutableStateFlow(prefs.getBoolean("onboarded", false))
    val onboarded = _onboarded.asStateFlow()

    fun finishOnboarding() {
        prefs.edit().putBoolean("onboarded", true).apply(); _onboarded.value = true
    }

    fun widgetHintNeeded(context: android.content.Context): Boolean {
        if (prefs.getBoolean("widget_hint_dismissed", false)) return false
        val added = android.appwidget.AppWidgetManager.getInstance(context).getAppWidgetIds(
            android.content.ComponentName(context, HydrationWidgetProvider::class.java)).isNotEmpty()
        if (added) return false
        val first = prefs.getString(KEY_FIRST_LAUNCH, null)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return false
        return java.time.temporal.ChronoUnit.DAYS.between(first, LocalDate.now()) >= 3
    }

    fun dismissWidgetHint() { prefs.edit().putBoolean("widget_hint_dismissed", true).apply() }

    fun shouldPromptReview(): Boolean {
        val s = streak.value
        val today = LocalDate.now().toString()
        if (s >= 7 && s % 7 == 0 && prefs.getString("last_review_day", "") != today) {
            prefs.edit().putString("last_review_day", today).apply(); return true
        }
        return false
    }

    // ---------- Session state ----------
    private val _session = MutableStateFlow<ActiveSession?>(null)
    val session = _session.asStateFlow()

    private val _lastSummary = MutableStateFlow<SessionSummary?>(null)
    val lastSummary = _lastSummary.asStateFlow()

    // ---------- Water logging ----------
    fun addWater(amountMl: Int) = viewModelScope.launch {
        dao.insert(WaterEntry(amountMl = amountMl.coerceIn(1, 5000)))
        HydrationWidgetProvider.updateAll(appContext)
    }

    fun deleteEntry(entry: WaterEntry) = viewModelScope.launch {
        dao.delete(entry)
        HydrationWidgetProvider.updateAll(appContext)
    }

    fun undoLast() {
        allEntries.value.filter { it.isToday() }.maxByOrNull { it.timestamp }?.let(::deleteEntry)
    }

    // ---------- Settings API ----------
    fun setDailyGoal(goalMl: Int) {
        prefs.edit().putInt(KEY_GOAL, goalMl).apply(); _dailyGoal.value = goalMl
    }
    fun setUnit(unit: VolumeUnit) {
        prefs.edit().putString(KEY_UNIT, unit.name).apply(); _unit.value = unit
    }
    fun updateCups(cups: List<CupPreset>) {
        val cleaned = cups.filter { it.ml > 0 }.ifEmpty { DEFAULT_CUPS }.take(6)
        prefs.edit().putString(KEY_CUPS, cupsToJson(cleaned)).apply(); _cups.value = cleaned
    }
    fun saveSweatRate(lph: Double) {
        prefs.edit().putFloat(KEY_SWEAT, lph.toFloat()).apply(); _sweatRate.value = lph
    }

    // ---------- Sessions ----------
    fun startSession(templateId: String, intensity: Intensity, minutes: Int) = viewModelScope.launch {
        ReminderScheduler.cancelAll(appContext)
        val t = template(templateId)
        val rate = _sweatRate.value.takeIf { it > 0 }

        var planned = suggestedMl(t, intensity, minutes, rate)
        var every = t.sipEveryMin
        if (_hotDay.value) { val (p, e) = applyHotDay(planned, every); planned = p; every = e }
        val plan = if (t.mode == SessionMode.NIGHT_OUT) SipPlan(every, 0, DRINK_MATCH_ML)
                   else sipPlan(minutes, planned, every)

        val now = System.currentTimeMillis()
        val id = dao.insertSession(Session(
            templateId = templateId, intensity = intensity.name, durationMin = minutes,
            startedAt = now, suggestedMl = planned, hotDay = _hotDay.value,
            sweatMl = rate?.let { (it * 1000 * minutes / 60.0).roundToInt() }))

        val session = ActiveSession(id, templateId, intensity, minutes, now, plan, planned, t.mode)
        _session.value = session
        sessionStore.save(session)
        SessionEndWorker.schedule(appContext, id, minutes + 1L)

        if (t.mode == SessionMode.STANDARD) {
            repeat(plan.count) { i ->
                val atMs = now + (i + 1) * plan.everyMin * 60_000L
                ReminderScheduler.schedule(appContext, (i + 1) * plan.everyMin.toLong(),
                    plan.mlPerSip, "Sip time 💧", "sip", atMs)
            }
        }
        if (!t.recoveryPlan) t.endAdvice?.let {
            val atMs = now + (minutes + 1) * 60_000L
            ReminderScheduler.schedule(appContext, minutes + 1L, 250, it, "arrival", atMs)
        }
    }

    fun logSip() { _session.value?.let { addWater(it.plan.mlPerSip) } }

    fun logDrink() {
        val s = _session.value ?: return
        val updated = s.copy(drinks = s.drinks + 1)
        _session.value = updated
        viewModelScope.launch { dao.addDrink(s.id); sessionStore.save(updated) }
        val atMs = System.currentTimeMillis() + 5 * 60_000L
        ReminderScheduler.schedule(appContext, 5, DRINK_MATCH_ML,
            "Match your drink — ${DRINK_MATCH_ML} ml water 💧", "drink", atMs)
    }

    fun endSession() {
        val s = _session.value ?: return
        viewModelScope.launch {
            SessionEndWorker.cancel(appContext, s.id)
            _lastSummary.value = SessionFinalizer.finalize(appContext, s)
            _session.value = null
            sessionStore.clear()
        }
    }

    fun rateLast(rpe: Int) = viewModelScope.launch { if (rpe in 1..10) dao.setLastRpe(rpe) }
    fun clearSummary() { _lastSummary.value = null }

    // ---------- Export ----------
    fun exportCsv(onReady: (Uri?) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        onReady(CsvExporter.export(appContext, allEntries.value, sessions.value))
    }

    // ---------- Deep link: hydrotracker://log?ml=250 ----------
    fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "hydrotracker" && data.host == "log") {
            val ml = data.getQueryParameter("ml")?.toIntOrNull() ?: 250
            if (ml > 0) addWater(ml)
        }
    }

    companion object {
        const val DEFAULT_GOAL = 2000
        const val ACTIVE_DAY_BONUS = 500
        const val HOT_DAY_MULTIPLIER = 1.25
        private const val KEY_GOAL = "daily_goal_ml"
        private const val KEY_UNIT = "unit"
        private const val KEY_CUPS = "cups_json"
        private const val KEY_SWEAT = "sweat_rate_lph"
        private const val KEY_PACING = "pacing_enabled"
        private const val KEY_TZ = "last_timezone"
        private const val KEY_TZ_CHANGES = "tz_change_days"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }
}
