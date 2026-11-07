package io.github.highlandev210.kisha.ui.entry

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
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
        runCatching {
            compose.waitUntil(10_000) { compose.onAllNodesWithText(expected).fetchSemanticsNodes().isNotEmpty() }
        }.onFailure {
            val shown = compose.onNodeWithTag(TestTags.ALLOWANCE).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)
            throw AssertionError("allowance never showed $expected; it shows $shown", it)
        }
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
