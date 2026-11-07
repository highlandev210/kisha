package com.example.kisha.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kisha.allowance.CycleStart
import com.example.kisha.data.db.KishaDatabase
import com.example.kisha.data.db.PeriodType
import com.example.kisha.testing.FixedTimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class BudgetRepositoryTest {
    private lateinit var db: KishaDatabase
    private lateinit var repo: BudgetRepository
    private val time = FixedTimeSource(Instant.parse("2026-11-07T04:30:00Z"))

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    private val categories = listOf(
        "Food" to "restaurant", "Transport" to "directions_bus", "Groceries" to "local_grocery_store",
        "Shopping" to "shopping_bag", "Bills" to "receipt_long", "Health" to "medical_services",
        "Entertainment" to "movie", "Other" to "category",
    ).map { (name, icon) -> DefaultCategory(name, icon) }

    private fun monthlySetup(cycle: CycleStart = CycleStart.DayOfMonth(1), type: PeriodType = PeriodType.MONTH) = SetupInput(
        periodType = type,
        cycleStart = cycle,
        baseIncomePaise = 6_000_000,
        fixedCosts = listOf(NewFixedCost("Rent", 3_000_000, 5), NewFixedCost("Phone", 600_000, null)),
    )

    @Before fun open() {
        db = KishaDatabase.inMemory(ApplicationProvider.getApplicationContext())
        repo = BudgetRepository(db, time)
    }

    @After fun close() = db.close()

    private suspend fun setUpNovember() = repo.completeSetup(monthlySetup(), categories, d(2026, 11, 7))

    @Test fun completeSetupWritesEverything() = runTest {
        setUpNovember()
        val config = repo.config.first()!!
        assertEquals(1, config.cycleStartDay)
        assertEquals(PeriodType.MONTH, config.periodType)
        val period = repo.periods.first().single()
        assertEquals(d(2026, 11, 1), period.startDate)
        assertEquals(d(2026, 11, 30), period.endDate)
        assertEquals(6_000_000, period.income)
        assertEquals(3_600_000, period.fixedTotal)
        assertEquals(0, period.savingsTarget)
        assertNull(period.closedAt)
        assertEquals(2, repo.fixedCosts.first().size)
        assertEquals(8, db.categoryDao().count())
    }

    @Test fun customPaydaySetup() = runTest {
        repo.completeSetup(monthlySetup(CycleStart.DayOfMonth(7), PeriodType.CUSTOM), categories, d(2026, 11, 7))
        val period = repo.periods.first().single()
        assertEquals(d(2026, 11, 7), period.startDate)
        assertEquals(d(2026, 12, 6), period.endDate)
        assertEquals(7, repo.config.first()!!.cycleStartDay)
    }

    @Test fun ensureCurrentPeriodIsNoOpInsidePeriod() = runTest {
        setUpNovember()
        assertEquals(d(2026, 11, 1), repo.ensureCurrentPeriod(d(2026, 11, 20))!!.startDate)
        assertEquals(1, db.periodDao().count())
    }

    @Test fun rolloverCopiesIncomeAndFixedCosts() = runTest {
        setUpNovember()
        val dec = repo.ensureCurrentPeriod(d(2026, 12, 3))!!
        assertEquals(d(2026, 12, 1), dec.startDate)
        assertEquals(d(2026, 12, 31), dec.endDate)
        assertEquals(6_000_000, dec.income)
        assertEquals(3_600_000, dec.fixedTotal)
        assertNull(dec.closedAt)
        val nov = db.periodDao().containing(d(2026, 11, 15))!!
        assertNotNull(nov.closedAt)
    }

    @Test fun rolloverFillsGapsAcrossMonths() = runTest {
        setUpNovember()
        val feb = repo.ensureCurrentPeriod(d(2027, 2, 10))!!
        assertEquals(d(2027, 2, 1), feb.startDate)
        assertEquals(d(2027, 2, 28), feb.endDate)
        assertEquals(
            listOf(d(2027, 2, 1), d(2027, 1, 1), d(2026, 12, 1), d(2026, 11, 1)),
            repo.periods.first().map { it.startDate },
        )
    }

    @Test fun concurrentEnsureCreatesOnePeriod() = runTest {
        setUpNovember()
        coroutineScope {
            repeat(5) { launch(Dispatchers.IO) { repo.ensureCurrentPeriod(d(2026, 12, 3)) } }
        }
        assertEquals(2, db.periodDao().count())
    }

    @Test fun ensureCurrentPeriodDoesNothingWhenTodayBeforeLatest() = runTest {
        setUpNovember()
        assertEquals(d(2026, 11, 1), repo.ensureCurrentPeriod(d(2026, 10, 20))!!.startDate)
        assertEquals(1, db.periodDao().count())
    }

    @Test fun fixedCostChangeUpdatesOpenPeriodOnly() = runTest {
        setUpNovember()
        repo.ensureCurrentPeriod(d(2026, 12, 3))
        repo.addFixedCost(NewFixedCost("Gym", 100_000, null))
        assertEquals(3_700_000, db.periodDao().containing(d(2026, 12, 3))!!.fixedTotal)
        assertEquals(3_600_000, db.periodDao().containing(d(2026, 11, 3))!!.fixedTotal)
    }

    @Test fun baseIncomeEditUpdatesConfigAndOpenPeriod() = runTest {
        setUpNovember()
        repo.setBaseIncome(6_500_000)
        assertEquals(6_500_000, repo.config.first()!!.baseIncome)
        assertEquals(6_500_000, db.periodDao().containing(d(2026, 11, 7))!!.income)
    }

    @Test fun addIncomeAttachesToCurrentPeriod() = runTest {
        setUpNovember()
        repo.addIncome(200_000, "Bonus")
        val novId = db.periodDao().containing(d(2026, 11, 7))!!.id
        val incomes = repo.observeIncome(novId).first()
        assertEquals(1, incomes.size)
        assertEquals(200_000, incomes.single().amountPaise)
    }
}
