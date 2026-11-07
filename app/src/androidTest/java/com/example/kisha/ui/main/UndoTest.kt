package com.example.kisha.ui.main

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
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

@OptIn(ExperimentalTestApi::class) // performCustomAccessibilityActionWithLabel
@RunWith(AndroidJUnit4::class)
class UndoTest {
    @get:Rule val compose = createComposeRule()
    private val time = FixedTimeSource(Instant.parse("2026-11-01T04:30:00Z"))
    private lateinit var container: AppContainer
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun s(id: Int) = context.getString(id)

    @Before fun setUp() {
        container = testContainer(time)
        runBlocking {
            container.setUpWorkedExample()
            container.expenseRepository.add(20_000, null, "lunch", time.now())
        }
        compose.setContent { KishaTheme { KishaAppUi(container) } }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(TestTags.ALLOWANCE).fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("lunch", substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    @After fun tearDown() = container.database.close()

    private fun awaitText(text: String, present: Boolean = true) = compose.waitUntil(5_000) {
        compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() == present
    }

    @Test fun undoSurvivesSwitchingTabs() {
        // The TalkBack "Delete" action lives on the swipeable row that contains the expense.
        compose.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions) and hasAnyDescendant(hasText("lunch", substring = true)),
        ).performCustomAccessibilityActionWithLabel(s(R.string.delete))
        awaitText("lunch", present = false)

        compose.onNodeWithText(s(R.string.tab_settings)).performClick()
        compose.onNodeWithText(s(R.string.undo)).performClick()
        compose.onNodeWithText(s(R.string.tab_today)).performClick()

        awaitText("lunch")
    }

    @Test fun deletingFromTheSheetCanBeUndone() {
        compose.onNodeWithText("lunch", substring = true).performClick()
        compose.onNodeWithText(s(R.string.entry_delete)).performClick()
        awaitText("lunch", present = false)

        compose.onNodeWithText(s(R.string.undo)).performClick()

        awaitText("lunch")
    }
}
