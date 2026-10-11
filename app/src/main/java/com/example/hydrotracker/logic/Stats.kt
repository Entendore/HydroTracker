package com.example.hydrotracker.logic

import com.example.hydrotracker.data.WaterEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

fun WaterEntry.localDate(): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
fun WaterEntry.isToday(): Boolean = localDate() == LocalDate.now()
private fun WaterEntry.hour(): Int =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).hour

fun dailyTotals(entries: List<WaterEntry>): Map<LocalDate, Int> =
    entries.groupBy { it.localDate() }.mapValues { (_, v) -> v.sumOf { it.amountMl } }

fun currentStreak(totals: Map<LocalDate, Int>, goal: Int, graceDays: Set<Long> = emptySet()): Int {
    var day = LocalDate.now()
    if ((totals[day] ?: 0) < goal && day.toEpochDay() !in graceDays) day = day.minusDays(1)
    var streak = 0
    while (true) {
        val total = totals[day] ?: 0
        when {
            total >= goal -> streak++
            day.toEpochDay() in graceDays -> Unit
            else -> break
        }
        day = day.minusDays(1)
    }
    return streak
}

fun bestStreakEver(totals: Map<LocalDate, Int>, goal: Int): Int {
    if (totals.isEmpty()) return 0
    var day = totals.keys.min(); var best = 0; var cur = 0
    while (!day.isAfter(LocalDate.now())) {
        if ((totals[day] ?: 0) >= goal) { cur++; best = max(best, cur) } else cur = 0
        day = day.plusDays(1)
    }
    return best
}

data class Badge(val id: String, val title: String, val emoji: String, val earned: Boolean)

fun buildBadges(entries: List<WaterEntry>, goal: Int): List<Badge> {
    if (goal <= 0) return emptyList()
    val totals = dailyTotals(entries)
    val goalDays = totals.count { it.value >= goal }
    val best = bestStreakEver(totals, goal)
    return listOf(
        Badge("first", "First Goal", "💧", goalDays >= 1),
        Badge("s3", "3-Day Streak", "🔥", best >= 3),
        Badge("s7", "Week Warrior", "🔥", best >= 7),
        Badge("s30", "Month Master", "🏆", best >= 30),
        Badge("d10", "10 Goal Days", "🥉", goalDays >= 10),
        Badge("d50", "50 Goal Days", "🥇", goalDays >= 50),
        Badge("big", "Big Gulp", "🌊", totals.values.any { it >= 3000 }),
        Badge("early", "Early Bird", "🌅", entries.any { it.hour() < 7 }),
    )
}

fun buildInsights(entries: List<WaterEntry>, goal: Int): List<String> {
    if (goal <= 0) return emptyList()
    val totals = dailyTotals(entries)
    val today = LocalDate.now()
    val last28 = (0 until 28).map { today.minusDays(it.toLong()) }
    if (last28.none { (totals[it] ?: 0) > 0 })
        return listOf("Log a few days of water to unlock personal insights.")

    val byWeekday = last28.groupBy { it.dayOfWeek }
        .mapValues { (_, days) -> days.map { (totals[it] ?: 0) }.average() }
    val best = byWeekday.maxByOrNull { it.value }!!
    val worst = byWeekday.minByOrNull { it.value }!!
    val fullDay = { d: java.time.DayOfWeek -> d.getDisplayName(TextStyle.FULL, Locale.getDefault()) }
    val avgMl = last28.map { totals[it] ?: 0 }.average().roundToInt()
    val topHour = entries.groupBy { it.hour() }.maxByOrNull { it.value.size }?.key

    return buildList {
        add("You've averaged ${formatMl(avgMl, VolumeUnit.ML)} per day over the last 4 weeks.")
        add("Your strongest day is ${fullDay(best.key)} (${best.value.roundToInt()} ml on average).")
        if (worst.value < best.value * 0.8)
            add("Watch out for ${fullDay(worst.key)} — that's your driest day of the week.")
        topHour?.let { add("You log most often around $it:00. Set a reminder for your quiet hours.") }
    }
}
