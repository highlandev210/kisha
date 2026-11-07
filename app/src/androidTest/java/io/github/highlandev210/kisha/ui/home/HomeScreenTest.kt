package io.github.highlandev210.kisha.ui.home

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.highlandev210.kisha.AppContainer
import io.github.highlandev210.kisha.R
import io.github.highlandev210.kisha.testing.FixedTimeSource
import io.github.highlandev210.kisha.testing.setUpWorkedExample
import io.github.highlandev210.kisha.testing.testContainer
import io.github.highlandev210.kisha.ui.KishaAppUi
import io.github.highlandev210.kisha.ui.TestTags
import io.github.highlandev210.kisha.ui.theme.KishaTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule val compose = createComposeRule()
    private val time = FixedTimeSource(Instant.parse("2026-11-01T04:30:00Z"))
    private lateinit var container: AppContainer
    private val res = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Before fun setUp() {
        container = testContainer(time)
        runBlocking { container.setUpWorkedExample() }
        compose.setContent { KishaTheme { KishaAppUi(container) } }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(TestTags.ALLOWANCE).fetchSemanticsNodes().isNotEmpty() }
    }

    @After fun tearDown() = container.database.close()

    @Test fun showsAllowanceAndPeriodSummary() {
        compose.onNodeWithTag(TestTags.ALLOWANCE).assertTextEquals("₹800")
        compose.onNodeWithText("₹24,000", substring = true).assertExists()
        compose.onNodeWithText(res.getQuantityString(R.plurals.home_days_to_go, 30)).assertExists()
        compose.onNodeWithText(res.getString(R.string.status_on_track)).assertExists()
    }

    @Test fun showsAnEmptyPocketOnSetupDay() {
        compose.onNodeWithText(res.getString(R.string.home_pocket_label)).assertExists()
        compose.onNodeWithText(res.getString(R.string.home_pocket_start)).assertExists()
    }
}
