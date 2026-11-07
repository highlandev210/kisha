package com.example.kisha.ui.entry

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class QuickEntryTest {
    @get:Rule val compose = createComposeRule()
    private val time = FixedTimeSource(Instant.parse("2026-11-01T04:30:00Z"))
    private lateinit var container: AppContainer
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun setUp() {
        container = testContainer(time)
        runBlocking { container.setUpWorkedExample() }
        compose.setContent { KishaTheme { KishaAppUi(container) } }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(TestTags.ALLOWANCE).fetchSemanticsNodes().isNotEmpty() }
    }

    @After fun tearDown() = container.database.close()

    private fun press(keys: String) = keys.forEach { c ->
        val tag = when (c) {
            '.' -> TestTags.KEY_DOT
            '<' -> TestTags.KEY_BACKSPACE
            else -> TestTags.key(c.digitToInt())
        }
        compose.onNodeWithTag(tag).performClick()
    }

    private fun assertAllowance(expected: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithText(expected).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(TestTags.ALLOWANCE).assertTextEquals(expected)
    }

    @Test fun addExpenseUpdatesToday() {
        compose.onNodeWithContentDescription(context.getString(R.string.add)).performClick()
        press("500")
        compose.onNodeWithTag(TestTags.ENTRY_SAVE).performClick()
        assertAllowance("₹300")
    }

    @Test fun editExpenseRecalculates() {
        runBlocking { container.expenseRepository.add(50_000, null, "", time.now()) }
        assertAllowance("₹300")
        compose.onNodeWithText(context.getString(R.string.expense_no_category)).performClick()
        press("<<<200")
        compose.onNodeWithTag(TestTags.ENTRY_SAVE).performClick()
        assertAllowance("₹600")
    }

    @Test fun incomeModeAddsIncome() {
        compose.onNodeWithContentDescription(context.getString(R.string.add)).performClick()
        compose.onNodeWithText(context.getString(R.string.entry_mode_income)).performClick()
        press("2000")
        compose.onNodeWithTag(TestTags.ENTRY_SAVE).performClick()
        assertAllowance("₹866")
    }
}
