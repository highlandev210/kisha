package io.github.highlandev210.kisha.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import io.github.highlandev210.kisha.allowance.CycleStart
import io.github.highlandev210.kisha.data.db.KishaDatabase
import io.github.highlandev210.kisha.data.db.PeriodType
import io.github.highlandev210.kisha.testing.FixedTimeSource
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ExpenseRepositoryTest {
    private lateinit var db: KishaDatabase
    private lateinit var budget: BudgetRepository
    private lateinit var expenses: ExpenseRepository
    private val time = FixedTimeSource(Instant.parse("2026-11-07T04:30:00Z"))

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    @Before fun open() = runTest {
        db = KishaDatabase.inMemory(ApplicationProvider.getApplicationContext())
        budget = BudgetRepository(db, time)
        expenses = ExpenseRepository(db, time)
        budget.completeSetup(
            SetupInput(PeriodType.MONTH, CycleStart.DayOfMonth(1), 6_000_000, listOf(NewFixedCost("Rent", 3_600_000, null))),
            listOf(DefaultCategory("Food", "restaurant")),
            d(2026, 11, 7),
        )
    }

    @After fun close() = db.close()

    private suspend fun periodIdOn(day: LocalDate) = db.periodDao().containing(day)!!.id

    @Test fun addAssignsPeriodByLocalDate() = runTest {
        val lateNov = expenses.add(10_000, null, "", Instant.parse("2026-11-30T18:00:00Z")) // 23:30 IST, Nov 30
        assertEquals(periodIdOn(d(2026, 11, 30)), expenses.get(lateNov)!!.periodId)

        budget.ensureCurrentPeriod(d(2026, 12, 1))
        val earlyDec = expenses.add(10_000, null, "", Instant.parse("2026-11-30T18:45:00Z")) // 00:15 IST, Dec 1
        assertEquals(periodIdOn(d(2026, 12, 1)), expenses.get(earlyDec)!!.periodId)
    }

    @Test fun updateMovesExpenseToNewPeriod() = runTest {
        budget.ensureCurrentPeriod(d(2026, 12, 3))
        val id = expenses.add(10_000, null, "", Instant.parse("2026-12-03T06:00:00Z"))
        time.currentInstant = Instant.parse("2026-12-03T09:00:00Z")

        expenses.update(expenses.get(id)!!.copy(occurredAt = Instant.parse("2026-11-15T06:00:00Z")))

        val moved = expenses.get(id)!!
        assertEquals(periodIdOn(d(2026, 11, 15)), moved.periodId)
        assertEquals(time.currentInstant, moved.updatedAt)
    }

    @Test fun deleteThenRestoreKeepsIdAndFields() = runTest {
        val id = expenses.add(12_345, null, "chai", time.now())
        val original = expenses.get(id)!!
        expenses.delete(original)
        expenses.restore(original)
        assertEquals(original, expenses.get(id))
    }

    @Test fun observeForPeriodEmitsOnChange() = runTest {
        val periodId = periodIdOn(d(2026, 11, 7))
        expenses.observeForPeriod(periodId).test {
            assertEquals(0, awaitItem().size)
            val id = expenses.add(5_000, null, "", time.now())
            assertEquals(1, awaitItem().size)
            expenses.delete(expenses.get(id)!!)
            assertEquals(0, awaitItem().size)
        }
    }

    @Test fun addBeforeFirstPeriodUsesEarliest() = runTest {
        val id = expenses.add(10_000, null, "", Instant.parse("2026-10-15T06:00:00Z"))
        assertEquals(periodIdOn(d(2026, 11, 1)), expenses.get(id)!!.periodId)
    }
}
