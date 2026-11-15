package com.aetherealmind.grannysknees.ui.today

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aetherealmind.grannysknees.ui.theme.GrannysKneesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UE-07 L2: Today honesty states with injected UI state — no live weather, so
 * stale/expired/offline cannot be mistaken for a lucky fresh score.
 */
@RunWith(AndroidJUnit4::class)
class TodayHonestyInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun freshAvailableShowsScoreWithoutStaleBadge() {
        composeRule.setContent {
            GrannysKneesTheme {
                TodayScreen(state = SampleTodayStates.fresh)
            }
        }

        composeRule.onNodeWithText("7.0", substring = true).assertIsDisplayed()
        composeRule.onAllNodesWithText("Stale", substring = true).assertCountEquals(0)
    }

    @Test
    fun staleAvailableShowsStaleLabellingAndKeepsScore() {
        composeRule.setContent {
            GrannysKneesTheme {
                TodayScreen(state = SampleTodayStates.stale)
            }
        }

        composeRule.onNodeWithText("4.0", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Stale", substring = true).assertIsDisplayed()
    }

    @Test
    fun expiredHidesIndexAndShowsExpiredCopy() {
        composeRule.setContent {
            GrannysKneesTheme {
                TodayScreen(state = SampleTodayStates.expired)
            }
        }

        composeRule.onNodeWithText("Forecast expired", substring = true).assertIsDisplayed()
        composeRule.onAllNodesWithText("9.9").assertCountEquals(0)
    }

    @Test
    fun offlineEmptyShowsNoForecastCopy() {
        composeRule.setContent {
            GrannysKneesTheme {
                TodayScreen(state = SampleTodayStates.offline)
            }
        }

        composeRule.onNodeWithText("No forecast just yet", substring = true).assertIsDisplayed()
    }

    @Test
    fun invalidHourShowsNoScoreThisHour() {
        val invalid = TodayUiState.Unavailable(
            location = "Toronto, ON",
            lastUpdatedLabel = "today at 8:45 AM",
            reason = UnavailableReason.INVALID_HOUR,
        )
        composeRule.setContent {
            GrannysKneesTheme {
                TodayScreen(state = invalid)
            }
        }

        composeRule.onNodeWithText("No score this hour", substring = true).assertIsDisplayed()
    }
}
