package com.example.hydrotracker.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.hydrotracker.MainActivity
import com.example.hydrotracker.SeedRule
import com.example.hydrotracker.logic.VolumeUnit
import com.example.hydrotracker.logic.formatMl
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CupLoggingUiTest {

    @get:ClassRule @JvmField val seed = SeedRule()
    @get:Rule @JvmField val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun tappingCup_incrementsTodayTotal() {
        val baseGoal = "of ${formatMl(2000, VolumeUnit.ML)} goal"
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(baseGoal).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(baseGoal).assertExists()

        compose.onNodeWithTag("cup_200").performClick()

        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("200").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("200").assertExists()
    }
}

@RunWith(AndroidJUnit4::class)
class HotDayToggleUiTest {

    @get:ClassRule @JvmField val seed = SeedRule()
    @get:Rule @JvmField val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun togglingHotDay_raisesEffectiveGoalBy25Percent() {
        val base = "of ${formatMl(2000, VolumeUnit.ML)} goal"
        val boosted = "of ${formatMl(2500, VolumeUnit.ML)} goal"

        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(base).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(base).assertExists()

        compose.onNodeWithTag("chip_hot").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(boosted).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(boosted).assertExists()

        compose.onNodeWithTag("chip_hot").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(base).fetchSemanticsNodes().isNotEmpty()
        }
    }
}

@RunWith(AndroidJUnit4::class)
class SessionFlowUiTest {

    @get:ClassRule @JvmField val seed = SeedRule()
    @get:Rule @JvmField val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun startLogSipEndSession_showsSummary() {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("tab_session").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("tab_session").performClick()

        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("start_session").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("start_session").performClick()

        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Running in progress").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithTag("log_sip").performClick()
        compose.onNodeWithTag("end_session").performClick()

        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Session complete", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("You drank 120 ml of the planned 240 ml.").assertExists()
        compose.onNodeWithText("Recovery plan: ~360 ml over the next 2 hours",
            substring = true).assertExists()
    }
}
