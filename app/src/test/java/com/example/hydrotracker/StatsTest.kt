package com.example.hydrotracker

import com.example.hydrotracker.data.WaterEntry
import com.example.hydrotracker.logic.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StatsTest {
    private fun e(day: LocalDate, ml: Int) =
        WaterEntry(amountMl = ml, timestamp = day.atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli())

    @Test fun `today below goal does not break streak`() {
        val totals = dailyTotals(listOf(
            e(LocalDate.now(), 1500),
            e(LocalDate.now().minusDays(1), 2000),
            e(LocalDate.now().minusDays(2), 2000)))
        assertEquals(2, currentStreak(totals, 2000))
    }

    @Test fun `grace day neither breaks nor extends`() {
        val totals = dailyTotals(listOf(
            e(LocalDate.now(), 1500),
            e(LocalDate.now().minusDays(2), 2000)))
        val grace = setOf(LocalDate.now().minusDays(1).toEpochDay())
        assertEquals(1, currentStreak(totals, 2000, grace))
    }

    @Test fun `best streak crosses month boundary`() {
        val first = LocalDate.now().withDayOfMonth(1)
        val entries = listOf(e(first, 2000), e(first.minusDays(1), 2000))
        assertEquals(2, bestStreakEver(dailyTotals(entries), 2000))
    }

    @Test fun `first goal badge`() {
        assertTrue(buildBadges(listOf(e(LocalDate.now(), 2000)), 2000)
            .first { it.id == "first" }.earned)
    }
}
