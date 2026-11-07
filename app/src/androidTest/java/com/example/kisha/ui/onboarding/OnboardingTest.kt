package com.example.kisha.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.kisha.AppContainer
import com.example.kisha.R
import com.example.kisha.testing.FixedTimeSource
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
class OnboardingTest {
    @get:Rule val compose = createComposeRule()

    private val time = FixedTimeSource(Instant.parse("2026-11-01T04:30:00Z")) // 10:00 IST, Nov 1
    private lateinit var container: AppContainer

    private fun s(@StringRes id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Before fun setUp() {
        container = testContainer(time)
        compose.setContent { KishaTheme { KishaAppUi(container) } }
    }

    @After fun tearDown() = container.database.close()

    private fun enterIncomeAndMonth(income: String) {
        compose.onNodeWithTag(TestTags.INCOME_FIELD).performTextInput(income)
        compose.onNodeWithText(s(R.string.onboarding_next)).performClick()
        compose.onNodeWithText(s(R.string.cycle_calendar_month)).performClick()
        compose.onNodeWithText(s(R.string.onboarding_next)).performClick()
    }

    private fun addFixedCost(name: String, amount: String) {
        compose.onNodeWithTag(TestTags.FIXED_NAME_FIELD).performTextInput(name)
        compose.onNodeWithTag(TestTags.FIXED_AMOUNT_FIELD).performTextInput(amount)
        compose.onNodeWithText(s(R.string.fixed_add)).performClick()
    }

    @Test fun onboardingCompletesAndShowsPreview() {
        enterIncomeAndMonth("60000")
        addFixedCost("Rent", "36000")
        compose.onNodeWithText(s(R.string.onboarding_next)).performClick()

        compose.onNodeWithTag(TestTags.PREVIEW_ALLOWANCE).assertTextEquals("₹800")

        compose.onNodeWithText(s(R.string.onboarding_start)).performClick()
        compose.waitUntil(5_000) { runBlocking { container.budgetRepository.config.first() } != null }
        assertEquals(6_000_000L, runBlocking { container.budgetRepository.config.first() }!!.baseIncome)
    }

    @Test fun incomeRequiredBeforeNext() {
        val next = compose.onNodeWithText(s(R.string.onboarding_next))
        next.assertIsNotEnabled()
        compose.onNodeWithTag(TestTags.INCOME_FIELD).performTextInput("0")
        next.assertIsNotEnabled()
        compose.onNodeWithTag(TestTags.INCOME_FIELD).performTextReplacement("5")
        next.assertIsEnabled()
    }

    @Test fun fixedAboveIncomePreviewShowsNegative() {
        enterIncomeAndMonth("30000")
        addFixedCost("Rent", "36000")
        compose.onNodeWithText(s(R.string.onboarding_next)).performClick()

        compose.onNodeWithTag(TestTags.PREVIEW_ALLOWANCE).assertTextEquals("-₹200")
    }

    @Test fun systemBackGoesToPreviousStep() {
        compose.onNodeWithTag(TestTags.INCOME_FIELD).performTextInput("60000")
        compose.onNodeWithText(s(R.string.onboarding_next)).performClick()
        compose.onNodeWithText(s(R.string.cycle_calendar_month)).assertExists()

        Espresso.closeSoftKeyboard()
        Espresso.pressBack()

        compose.onNodeWithTag(TestTags.INCOME_FIELD).assertTextContains("60000")
    }

    /** Typing a fixed cost and tapping the main button (without "Add") must not lose it. */
    @Test fun typedFixedCostIsKeptWhenTappingNext() {
        enterIncomeAndMonth("60000")
        compose.onNodeWithTag(TestTags.FIXED_NAME_FIELD).performTextInput("Rent")
        compose.onNodeWithTag(TestTags.FIXED_AMOUNT_FIELD).performTextInput("36000")
        compose.onNodeWithText(s(R.string.onboarding_next)).performClick()

        compose.onNodeWithTag(TestTags.PREVIEW_ALLOWANCE).assertTextEquals("₹800")
    }

    @Test fun incompleteFixedCostBlocksNext() {
        enterIncomeAndMonth("60000")
        compose.onNodeWithTag(TestTags.FIXED_NAME_FIELD).performTextInput("Rent")
        compose.onNodeWithText(s(R.string.onboarding_next)).performClick()

        compose.onNodeWithTag(TestTags.FIXED_NAME_FIELD).assertExists()
        compose.onAllNodesWithTag(TestTags.PREVIEW_ALLOWANCE).assertCountEquals(0)
        compose.onNodeWithText(s(R.string.fixed_invalid)).assertExists()
    }
}
