package com.example.kisha.ui.home

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.kisha.AppContainer
import com.example.kisha.testing.FixedTimeSource
import com.example.kisha.testing.awaitMatching
import com.example.kisha.testing.setUpWorkedExample
import com.example.kisha.testing.testContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HomeViewModelTest {
    private val time = FixedTimeSource(Instant.parse("2026-11-01T04:30:00Z"))
    private val today = MutableStateFlow(LocalDate.of(2026, 11, 1))
    private lateinit var container: AppContainer
    private lateinit var vm: HomeViewModel

    @Before fun setUp() {
        container = testContainer(time, today)
        runBlocking { container.setUpWorkedExample() }
        vm = HomeViewModel(container.budgetRepository, container.expenseRepository, today, time)
    }

    @After fun tearDown() = container.database.close()

    private val ready get() = vm.state.filterIsInstance<HomeUiState.Ready>()

    @Test fun allowanceUpdatesOnAddAndAtMidnight() = runTest {
        ready.test {
            assertEquals(80_000L, awaitItem().allowance.remainingPaise)
            container.expenseRepository.add(50_000, null, "", time.now())
            awaitMatching { it.allowance.remainingPaise == 30_000L }
            today.value = LocalDate.of(2026, 11, 2)
            assertEquals(81_034L, awaitMatching { it.allowance.day == LocalDate.of(2026, 11, 2) }.allowance.basePaise)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun midnightRolloverMovesToNextPeriod() = runTest {
        ready.test {
            awaitItem()
            today.value = LocalDate.of(2026, 12, 1)
            val dec = awaitMatching { it.period.start == LocalDate.of(2026, 12, 1) }
            assertEquals(LocalDate.of(2026, 12, 31), dec.period.endInclusive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun deleteAndUndoRestores() = runTest {
        container.expenseRepository.add(20_000, null, "lunch", time.now())
        ready.test {
            val withExpense = awaitMatching { it.todayExpenses.size == 1 }
            val lunch = withExpense.todayExpenses.single().expense
            vm.delete(lunch)
            awaitMatching { it.todayExpenses.isEmpty() }
            vm.restore(lunch)
            assertEquals("lunch", awaitMatching { it.todayExpenses.size == 1 }.todayExpenses.single().expense.note)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Two quick swipes queue two snackbars; Undo on the first must bring back the first expense. */
    @Test fun undoAfterTwoDeletesRestoresTheRightExpense() = runTest {
        container.expenseRepository.add(20_000, null, "lunch", time.now())
        container.expenseRepository.add(5_000, null, "chai", time.now().plusSeconds(60))
        ready.test {
            val both = awaitMatching { it.todayExpenses.size == 2 }.todayExpenses.map { it.expense }
            val lunch = both.single { it.note == "lunch" }
            val chai = both.single { it.note == "chai" }
            vm.delete(lunch)
            vm.delete(chai)
            awaitMatching { it.todayExpenses.isEmpty() }
            vm.restore(lunch)
            assertEquals(listOf("lunch"), awaitMatching { it.todayExpenses.size == 1 }.todayExpenses.map { it.expense.note })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun pocketIsEmptyOnSetupDay() = runTest {
        ready.test {
            val first = awaitItem()
            assertEquals(0L, first.pocket.amountPaise)
            assertEquals(null, first.celebration)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun yesterdaysLeftoverDropsIntoThePocketOnce() = runTest {
        container.expenseRepository.add(50_000, null, "", time.now()) // ₹500 of ₹800 on Nov 1
        today.value = LocalDate.of(2026, 11, 2)
        ready.test {
            val nextDay = awaitMatching { it.allowance.day == LocalDate.of(2026, 11, 2) && it.celebration != null }
            assertEquals(30_000L, nextDay.pocket.amountPaise)
            assertEquals(PocketCelebration(LocalDate.of(2026, 11, 1), 30_000), nextDay.celebration)

            vm.markCelebrated(nextDay.celebration!!)
            assertEquals(30_000L, awaitMatching { it.celebration == null }.pocket.amountPaise)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun severalUnseenDaysCelebrateAsOne() = runTest {
        today.value = LocalDate.of(2026, 11, 3) // Nov 1 and 2 untouched: +₹800 and +₹827
        ready.test {
            val c = awaitMatching { it.allowance.day == LocalDate.of(2026, 11, 3) && it.celebration != null }.celebration!!
            assertEquals(LocalDate.of(2026, 11, 2), c.throughDay)
            assertEquals(80_000L + 82_758L, c.deltaPaise)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
