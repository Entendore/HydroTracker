package com.example.hydrotracker

import com.example.hydrotracker.logic.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class TipsTest {
    @Test fun `tip of day is stable per day and differs across days`() {
        val a = tipOfTheDay(LocalDate.of(2025, 6, 1))
        assertEquals(a, tipOfTheDay(LocalDate.of(2025, 6, 1)))
        assertNotEquals(a, tipOfTheDay(LocalDate.of(2025, 6, 2)))
    }

    @Test fun `contextual excludes today tip and honors session tags`() {
        val tips = contextualTips(hotDay = false, activeDay = false, hour = 10,
            sessionTemplateId = "commute")
        assertEquals(3, tips.size)
        assertFalse(tips.any { it.id == HEALTH_TIPS[tipOfTheDay()].id })
        assertTrue(tips.any { it.tags.contains("travel") })
    }
}
