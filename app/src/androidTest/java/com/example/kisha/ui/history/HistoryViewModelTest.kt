package com.example.kisha.ui.history

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.kisha.AppContainer
import com.example.kisha.allowance.AllowanceStatus
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
class HistoryViewModelTest {
    private val time = FixedTimeSource(Instant.parse("2026-11-03T04:30:00Z"))
    private val today = MutableStateFlow(LocalDate.of(2026, 11, 3))
    private lateinit var container: AppContainer
    private lateinit var vm: HistoryViewModel

    private fun d(m: Int, day: Int) = LocalDate.of(2026, m, day)

    @Before fun setUp() {
        container = testContainer(time, today)
        runBlocking {
            container.setUpWorkedExample(d(11, 1))
            container.expenseRepository.add(50_000, null, "", Instant.parse("2026-11-01T04:30:00Z"))
            container.expenseRepository.add(100_000, null, "", Instant.parse("2026-11-02T04:30:00Z"))
        }
        vm = HistoryViewModel(container.budgetRepository, container.expenseRepository, today, time)
    }

    @After fun tearDown() = container.database.close()

    private val ready get() = vm.state.filterIsInstance<HistoryUiState.Ready>()

    @Test fun daysNewestFirstWithAllowance() = runTest {
        ready.test {
            val days = awaitMatching { s -> s.days.sumOf { it.expenses.size } == 2 }.days
            assertEquals(listOf(d(11, 3), d(11, 2), d(11, 1)), days.map { it.allowance.day })

            val (nov3, nov2, nov1) = days.map { it.allowance }
            assertEquals(80_000L, nov1.basePaise)
            assertEquals(50_000L, nov1.spentPaise)
            assertEquals(AllowanceStatus.ON_TRACK, nov1.status)
            assertEquals(81_034L, nov2.basePaise)
            assertEquals(-18_966L, nov2.remainingPaise)
            assertEquals(AllowanceStatus.OVER, nov2.status)
            assertEquals(80_357L, nov3.basePaise)
        }
    }

    @Test fun previousPeriodBrowsable() = runTest {
        today.value = d(12, 2)
        ready.test {
            awaitMatching { it.selected.startDate == d(12, 1) }
            vm.previous()
            val nov = awaitMatching { it.selected.startDate == d(11, 1) }
            assertEquals(30, nov.days.size)
            assertEquals(d(11, 30), nov.days.first().allowance.day)
            assertEquals(d(11, 1), nov.days.last().allowance.day)
        }
    }
}
