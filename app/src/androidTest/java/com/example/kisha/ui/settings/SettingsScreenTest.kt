package com.example.kisha.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.kisha.AppContainer
import com.example.kisha.R
import com.example.kisha.testing.FixedTimeSource
import com.example.kisha.testing.setUpWorkedExample
import com.example.kisha.testing.testContainer
import com.example.kisha.ui.KishaAppUi
import com.example.kisha.ui.TestTags
import com.example.kisha.ui.theme.KishaTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val time = FixedTimeSource(Instant.parse("2026-11-01T04:30:00Z"))
    private lateinit var container: AppContainer
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun s(id: Int, vararg args: Any) = context.getString(id, *args)

    @Before fun setUp() {
        container = testContainer(time)
        runBlocking { container.setUpWorkedExample() }
        compose.setContent { KishaTheme { KishaAppUi(container) } }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(TestTags.ALLOWANCE).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(s(R.string.tab_settings)).performClick()
    }

    @After fun tearDown() = container.database.close()

    private fun fixedCosts() = runBlocking { container.budgetRepository.fixedCosts.first() }

    @Test fun deletingAFixedCostAsksFirst() {
        compose.onNodeWithContentDescription(s(R.string.settings_delete_named, "Rent")).performClick()
        compose.onNodeWithText(s(R.string.settings_confirm_delete_title, "Rent")).assertExists()

        compose.onNodeWithText(s(R.string.dialog_cancel)).performClick()
        assertEquals(1, fixedCosts().size)

        compose.onNodeWithContentDescription(s(R.string.settings_delete_named, "Rent")).performClick()
        compose.onNodeWithText(s(R.string.delete)).performClick()
        compose.waitUntil(5_000) { fixedCosts().isEmpty() }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Rent").fetchSemanticsNodes().isEmpty() }
    }
}
