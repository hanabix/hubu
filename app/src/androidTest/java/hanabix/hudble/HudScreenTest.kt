package hanabix.hudble

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HUDScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun displaysHUDSectionsFromReadme() {
        composeTestRule.onNodeWithTag("pace_value").fetchSemanticsNode()
        composeTestRule.onNodeWithTag("heart_rate_value").fetchSemanticsNode()
        composeTestRule.onNodeWithTag("cadence_value").fetchSemanticsNode()
        composeTestRule.onNodeWithTag("current_time").assertTextContains("15:47")
        composeTestRule.onNodeWithTag("device_name").assertTextContains("Enduro 2")
        // 电量是动态获取的,只检查标签存在,不检查具体值
        composeTestRule.onNodeWithTag("battery_level").fetchSemanticsNode()
    }

    @Test
    fun displaysPreviewMetricValuesAndUnits() {
        composeTestRule.onNodeWithContentDescription("Pace").fetchSemanticsNode()
        composeTestRule.onNodeWithContentDescription("Heart Rate").fetchSemanticsNode()
        composeTestRule.onNodeWithContentDescription("Cadence").fetchSemanticsNode()
        composeTestRule.onNodeWithTag("pace_value").assertTextContains("6'21\"")
        composeTestRule.onNodeWithTag("heart_rate_value").assertTextContains("156")
        composeTestRule.onNodeWithTag("cadence_value").assertTextContains("178")
    }
}
