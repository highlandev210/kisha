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
        }
    }

    @Test fun midnightRolloverMovesToNextPeriod() = runTest {
        ready.test {
            awaitItem()
            today.value = LocalDate.of(2026, 12, 1)
            val dec = awaitMatching { it.period.start == LocalDate.of(2026, 12, 1) }
            assertEquals(LocalDate.of(2026, 12, 31), dec.period.endInclusive)
        }
    }

    @Test fun deleteAndUndoRestores() = runTest {
        container.expenseRepository.add(20_000, null, "lunch", time.now())
        ready.test {
            val withExpense = awaitMatching { it.todayExpenses.size == 1 }
            vm.delete(withExpense.todayExpenses.single().expense)
            awaitMatching { it.todayExpenses.isEmpty() }
            vm.undoDelete()
            assertEquals("lunch", awaitMatching { it.todayExpenses.size == 1 }.todayExpenses.single().expense.note)
        }
    }
}
