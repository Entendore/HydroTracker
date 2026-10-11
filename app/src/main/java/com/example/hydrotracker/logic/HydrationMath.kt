package com.example.hydrotracker.logic

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.max
import kotlin.math.roundToInt

enum class Intensity(val label: String, val multiplier: Double) {
    LOW("Low", 0.75), MODERATE("Moderate", 1.0), HIGH("High", 1.4)
}
enum class TemplateCategory(val label: String) {
    EXERCISE("Exercise"), WORK_PLAY("Work & Play"),
    TRAVEL("Travel & Outdoors"), LIFESTYLE("Lifestyle")
}
enum class ElectrolyteRule { NONE, IF_INTENSE, ALWAYS }
enum class SessionMode { STANDARD, NIGHT_OUT }

data class SessionTemplate(
    val id: String, val label: String, val icon: ImageVector, val category: TemplateCategory,
    val mlPerMin: Double, val sipEveryMin: Int, val durationPresets: List<Int>,
    val minMin: Int, val maxMin: Int, val preMl: Int = 0, val preAdvice: String? = null,
    val endAdvice: String? = null, val electrolyteRule: ElectrolyteRule = ElectrolyteRule.NONE,
    val recoveryPlan: Boolean = false, val mode: SessionMode = SessionMode.STANDARD
)

fun template(id: String): SessionTemplate = SESSION_TEMPLATES.first { it.id == id }

val SESSION_TEMPLATES = listOf(
    SessionTemplate("running", "Running", Icons.Filled.DirectionsRun, TemplateCategory.EXERCISE, 8.0, 15,
        listOf(30, 45, 60), 15, 120, 400, "Drink 400 ml in the 90 min before you start.",
        "Rehydrate steadily over the next 2 hours.", ElectrolyteRule.IF_INTENSE, true),
    SessionTemplate("cycling", "Cycling", Icons.Filled.DirectionsBike, TemplateCategory.EXERCISE, 7.0, 15,
        listOf(45, 60, 90), 15, 180, 400, "Drink 400 ml in the 90 min before you start.",
        "Rehydrate steadily over the next 2 hours.", ElectrolyteRule.IF_INTENSE, true),
    SessionTemplate("gym", "Gym", Icons.Filled.FitnessCenter, TemplateCategory.EXERCISE, 6.0, 15,
        listOf(45, 60, 90), 15, 150, 300, "Drink 300 ml before your first set.",
        "Rehydrate steadily over the next 2 hours.", ElectrolyteRule.IF_INTENSE, true),
    SessionTemplate("yoga", "Yoga", Icons.Filled.SelfImprovement, TemplateCategory.EXERCISE, 3.0, 20,
        listOf(30, 60, 75), 15, 120, 200, null, null),
    SessionTemplate("walking", "Walking", Icons.Filled.DirectionsWalk, TemplateCategory.EXERCISE, 4.0, 20,
        listOf(30, 60, 120), 15, 240, 0, null, null),
    SessionTemplate("swimming", "Swimming", Icons.Filled.Pool, TemplateCategory.EXERCISE, 7.0, 20,
        listOf(30, 45, 60), 15, 120, 200, null,
        "You sweat more than you notice in the pool — rehydrate now.", ElectrolyteRule.IF_INTENSE, true),
    SessionTemplate("desk", "Desk work", Icons.Filled.Work, TemplateCategory.WORK_PLAY, 4.0, 25,
        listOf(60, 120, 240), 25, 480, 0,
        "Pomodoro-style: a sip reminder every 25-min work block.", null),
    SessionTemplate("gaming", "Gaming", Icons.Filled.SportsEsports, TemplateCategory.WORK_PLAY, 3.0, 30,
        listOf(30, 45, 60, 90, 120), 15, 240, 0, "One sip per match, every match.", null),
    SessionTemplate("commute", "Commute", Icons.Filled.Commute, TemplateCategory.TRAVEL, 1.5, 30,
        listOf(15, 30, 45, 60), 10, 120, 500, "Fill a bottle before you leave.",
        "You've arrived — grab a glass of water."),
    SessionTemplate("hike", "Hike", Icons.Filled.Terrain, TemplateCategory.TRAVEL, 10.0, 20,
        listOf(60, 120, 180, 240), 30, 480, 500,
        "Pack more than you think you need — check the plan below.",
        "Long hike done — rebuild over the next 2 hours.", ElectrolyteRule.IF_INTENSE, true),
    SessionTemplate("flight", "Flight", Icons.Filled.FlightTakeoff, TemplateCategory.TRAVEL, 4.2, 45,
        listOf(60, 120, 180, 240, 300, 360, 480), 60, 720, 0,
        "Cabin air is dry — that's ~250 ml per hour in the air.",
        "Landed? Keep sipping for another hour."),
    SessionTemplate("sauna", "Sauna / hot yoga", Icons.Filled.HotTub, TemplateCategory.LIFESTYLE, 15.0, 10,
        listOf(15, 30, 45, 60), 10, 90, 500, "Drink 500 ml about 30 minutes before.",
        "Rehydrate slowly over the next hour.", ElectrolyteRule.ALWAYS, true),
    SessionTemplate("night", "Night out", Icons.Filled.LocalBar, TemplateCategory.LIFESTYLE, 2.0, 45,
        listOf(120, 180, 240, 300), 60, 360, 0,
        "One glass of water per drink — tap the beer button each round.",
        "One big glass of water before bed — future you says thanks.",
        ElectrolyteRule.NONE, false, SessionMode.NIGHT_OUT),
    SessionTemplate("yard", "Yard work", Icons.Filled.Yard, TemplateCategory.LIFESTYLE, 6.0, 30,
        listOf(30, 60, 120, 180), 15, 240, 0, null, null, ElectrolyteRule.IF_INTENSE, true),
)

fun suggestedMl(t: SessionTemplate, intensity: Intensity, minutes: Int,
                measuredSweatRateLph: Double? = null): Int {
    val base = (t.mlPerMin * intensity.multiplier * minutes).roundToInt()
    val rate = measuredSweatRateLph ?: return base
    if (rate <= 0 || t.mode == SessionMode.NIGHT_OUT) return base
    val sweatBased = (rate * 1000 * 0.85 * minutes / 60.0).roundToInt()
    return max(base, sweatBased)
}

data class SipPlan(val everyMin: Int, val count: Int, val mlPerSip: Int)

fun sipPlan(durationMin: Int, totalMl: Int, everyMin: Int = 15): SipPlan {
    val count = max(1, durationMin / everyMin)
    val mlPerSip = ((totalMl.toDouble() / count) / 10).roundToInt() * 10
    return SipPlan(everyMin, count, mlPerSip.coerceAtLeast(50))
}

fun applyHotDay(plannedMl: Int, everyMin: Int): Pair<Int, Int> =
    (plannedMl * 1.2).roundToInt() to max(10, (everyMin * 0.75).roundToInt())

fun needsElectrolytes(t: SessionTemplate, intensity: Intensity, minutes: Int,
                      sweatRateLph: Double?): Boolean = when (t.electrolyteRule) {
    ElectrolyteRule.ALWAYS -> true
    ElectrolyteRule.IF_INTENSE -> (intensity == Intensity.HIGH && minutes >= 45) ||
            minutes >= 120 || ((sweatRateLph ?: 0.0) * 1000 * minutes / 60.0) >= 700
    ElectrolyteRule.NONE -> false
}

data class RecoveryPlan(val totalMl: Int, val reminders: List<Pair<Long, Int>>)

fun recoveryPlan(sweatEstimateMl: Int): RecoveryPlan {
    val total = (sweatEstimateMl * 1.5).roundToInt().coerceIn(250, 3000)
    val shares = listOf(0.35, 0.35, 0.30).map { ((total * it) / 10.0).roundToInt() * 10 }
    return RecoveryPlan(total, listOf(30L to shares[0], 75L to shares[1], 120L to shares[2]))
}

const val DRINK_MATCH_ML = 300

fun sweatRateLph(weightLossKg: Double, fluidConsumedMl: Int, minutes: Int): Double =
    ((weightLossKg * 1000 + fluidConsumedMl) / (minutes / 60.0)) / 1000.0

enum class VolumeUnit { ML, OZ }

fun formatMl(ml: Int, unit: VolumeUnit): String = when (unit) {
    VolumeUnit.ML -> "%,d ml".format(ml)
    VolumeUnit.OZ -> "%.0f fl oz".format(ml / 29.5735)
}

fun toMl(value: Double, unit: VolumeUnit): Int = when (unit) {
    VolumeUnit.ML -> value.roundToInt()
    VolumeUnit.OZ -> (value * 29.5735).roundToInt()
}
