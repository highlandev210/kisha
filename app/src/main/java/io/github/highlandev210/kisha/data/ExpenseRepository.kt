package io.github.highlandev210.kisha.data

import io.github.highlandev210.kisha.allowance.localDateIn
import io.github.highlandev210.kisha.data.db.CategoryEntity
import io.github.highlandev210.kisha.data.db.ExpenseEntity
import io.github.highlandev210.kisha.data.db.KishaDatabase
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class ExpenseRepository(db: KishaDatabase, private val time: TimeSource) {
    private val expenseDao = db.expenseDao()
    private val periodDao = db.periodDao()

    val categories: Flow<List<CategoryEntity>> = db.categoryDao().observeVisible()

    fun observeForPeriod(periodId: Long): Flow<List<ExpenseEntity>> = expenseDao.observeForPeriod(periodId)

    suspend fun get(id: Long): ExpenseEntity? = expenseDao.get(id)

    suspend fun add(amountPaise: Long, categoryId: Long?, note: String, occurredAt: Instant): Long {
        val now = time.now()
        return expenseDao.insert(
            ExpenseEntity(
                periodId = periodIdFor(occurredAt),
                amountPaise = amountPaise,
                categoryId = categoryId,
                note = note,
                occurredAt = occurredAt,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun update(expense: ExpenseEntity) {
        expenseDao.update(expense.copy(periodId = periodIdFor(expense.occurredAt), updatedAt = time.now()))
    }

    suspend fun delete(expense: ExpenseEntity) = expenseDao.delete(expense)

    /** Undo for a delete: re-inserts the row with its original id. */
    suspend fun restore(expense: ExpenseEntity) {
        expenseDao.insert(expense)
    }

    /** The period containing the expense's local date; outside all periods, the nearest end. */
    private suspend fun periodIdFor(occurredAt: Instant): Long {
        val day = occurredAt.localDateIn(time.zone())
        periodDao.containing(day)?.let { return it.id }
        val earliest = checkNotNull(periodDao.earliest()) { "setup not complete" }
        return if (day.isBefore(earliest.startDate)) earliest.id else checkNotNull(periodDao.latest()).id
    }
}
