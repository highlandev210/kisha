package com.example.kisha.ui.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.kisha.AppContainer
import com.example.kisha.testing.FixedTimeSource
import com.example.kisha.testing.awaitMatching
import com.example.kisha.testing.setUpWorkedExample
import com.example.kisha.testing.testContainer
import com.example.kisha.ui.format.MoneyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class SettingsViewModelTest {
    private val time = FixedTimeSource(Instant.parse("2026-11-07T04:30:00Z"))
    private val today = MutableStateFlow(LocalDate.of(2026, 11, 7))
    private lateinit var container: AppContainer
    private lateinit var vm: SettingsViewModel

    @Before fun setUp() {
        container = testContainer(time, today)
        runBlocking { container.setUpWorkedExample(LocalDate.of(2026, 11, 7)) }
        vm = SettingsViewModel(container.budgetRepository, today)
    }

    @After fun tearDown() = container.database.close()

    @Test fun fixedCostAddRaisesReservedTotal() = runTest {
        assertTrue(vm.addFixedCost("Gym", "3000", ""))
        container.budgetRepository.periods.test {
            awaitMatching { it.single().fixedTotal == 3_900_000L }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun invalidFixedCostRejected() = runTest {
        assertFalse(vm.addFixedCost("", "100", ""))
        assertFalse(vm.addFixedCost("Gym", "0", ""))
        assertFalse(vm.addFixedCost("Gym", "100", "32"))
        assertEquals(1, container.budgetRepository.fixedCosts.first().size)
    }

    @Test fun baseIncomeEditRecalculates() = runTest {
        assertTrue(vm.setBaseIncome("66000"))
        container.budgetRepository.periods.test {
            awaitMatching { it.single().income == 6_600_000L }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun currencyChangePersists() = runTest {
        assertTrue(vm.setCurrency("$", false))
        lateinit var config: com.example.kisha.data.db.BudgetConfigEntity
        container.budgetRepository.config.test {
            config = awaitMatching { it?.currency == "$" }!!
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(false, config.indianGrouping)
        assertEquals("$100,000", MoneyFormatter(config.currency, config.indianGrouping).format(10_000_000))
    }
}
