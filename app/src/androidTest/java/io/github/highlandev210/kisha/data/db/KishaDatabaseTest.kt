package io.github.highlandev210.kisha.data.db

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
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
class KishaDatabaseTest {
    private lateinit var db: KishaDatabase
    private val t0 = Instant.parse("2026-11-07T03:30:00Z")

    @Before fun open() {
        db = KishaDatabase.inMemory(ApplicationProvider.getApplicationContext())
    }

    @After fun close() = db.close()

    private suspend fun insertNovember(): Long = db.periodDao().insert(
        PeriodEntity(
            startDate = LocalDate.of(2026, 11, 1),
            endDate = LocalDate.of(2026, 11, 30),
            income = 6_000_000,
            fixedTotal = 3_600_000,
            savingsTarget = 0,
        ),
    )

    private fun expense(periodId: Long, at: Instant, categoryId: Long? = null) = ExpenseEntity(
        periodId = periodId,
        amountPaise = 10_000,
        categoryId = categoryId,
        occurredAt = at,
        createdAt = at,
        updatedAt = at,
    )

    @Test fun periodContainingIsInclusive() = runTest {
        insertNovember()
        val dao = db.periodDao()
        assertNotNull(dao.containing(LocalDate.of(2026, 11, 1)))
        assertNotNull(dao.containing(LocalDate.of(2026, 11, 30)))
        assertNull(dao.containing(LocalDate.of(2026, 12, 1)))
    }

    @Test fun expensesForPeriodNewestFirst() = runTest {
        val periodId = insertNovember()
        val nine = t0
        val one = t0.plusSeconds(4 * 3600)
        val eleven = t0.plusSeconds(2 * 3600)
        listOf(nine, one, eleven).forEach { db.expenseDao().insert(expense(periodId, it)) }

        val order = db.expenseDao().observeForPeriod(periodId).first().map { it.occurredAt }
        assertEquals(listOf(one, eleven, nine), order)
    }

    @Test fun deletingCategoryNullsExpenseCategory() = runTest {
        val periodId = insertNovember()
        db.categoryDao().insertAll(listOf(CategoryEntity(name = "Food", icon = "restaurant", sortOrder = 0, isDefault = true)))
        val category = db.categoryDao().observeVisible().first().single()
        val expenseId = db.expenseDao().insert(expense(periodId, t0, category.id))

        db.categoryDao().delete(category)

        assertNull(db.expenseDao().get(expenseId)!!.categoryId)
    }

    @Test fun configRoundTrips() = runTest {
        val config = BudgetConfigEntity(periodType = PeriodType.CUSTOM, cycleStartDay = -1, baseIncome = 6_000_000)
        db.budgetConfigDao().upsert(config)
        assertEquals(config, db.budgetConfigDao().get())
    }
}
