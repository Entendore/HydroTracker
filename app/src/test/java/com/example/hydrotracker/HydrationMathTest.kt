package com.example.hydrotracker

import com.example.hydrotracker.logic.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HydrationMathTest {

    private val running = template("running")

    @Test fun `preset suggestion at moderate`() {
        assertEquals(360, suggestedMl(running, Intensity.MODERATE, 45, null))
    }

    @Test fun `measured sweat rate used only when higher`() {
        assertEquals(956, suggestedMl(running, Intensity.MODERATE, 45, 1.5))
        assertEquals(360, suggestedMl(running, Intensity.MODERATE, 45, 0.3))
    }

    @Test fun `sip plan splits into multiples of 10`() {
        val p = sipPlan(45, 360)
        assertEquals(3, p.count); assertEquals(120, p.mlPerSip)
        assertEquals(0, p.mlPerSip % 10)
    }

    @Test fun `sip plan floor is 50 ml`() {
        assertEquals(50, sipPlan(15, 30).mlPerSip)
    }

    @Test fun `hot day raises target and tightens cadence`() {
        val (ml, every) = applyHotDay(360, 15)
        assertEquals(432, ml); assertEquals(11, every)
        assertEquals(10, applyHotDay(360, 10).second)
    }

    @Test fun `sweat rate math`() {
        assertEquals(1.1, sweatRateLph(0.6, 500, 60), 1e-9)
    }

    @Test fun `electrolyte thresholds`() {
        assertTrue(needsElectrolytes(running, Intensity.HIGH, 45, null))
        assertFalse(needsElectrolytes(running, Intensity.MODERATE, 60, null))
        assertTrue(needsElectrolytes(running, Intensity.MODERATE, 60, 1.0))
        assertFalse(needsElectrolytes(template("yoga"), Intensity.HIGH, 90, null))
    }

    @Test fun `unit conversion`() {
        assertEquals(237, toMl(8.0, VolumeUnit.OZ))
        assertEquals(1000, toMl(1000.0, VolumeUnit.ML))
        assertTrue(formatMl(2000, VolumeUnit.OZ).endsWith("fl oz"))
    }
}
