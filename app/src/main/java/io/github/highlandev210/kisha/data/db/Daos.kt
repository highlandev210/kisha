package io.github.highlandev210.kisha.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface BudgetConfigDao {
    @Query("SELECT * FROM budget_config WHERE id = 1")
    fun observe(): Flow<BudgetConfigEntity?>

    @Query("SELECT * FROM budget_config WHERE id = 1")
    suspend fun get(): BudgetConfigEntity?

    @Upsert
    suspend fun upsert(config: BudgetConfigEntity)
}

@Dao
interface PeriodDao {
    @Insert
    suspend fun insert(period: PeriodEntity): Long

    @Update
    suspend fun update(period: PeriodEntity)

    @Query("SELECT * FROM period ORDER BY startDate DESC LIMIT 1")
    suspend fun latest(): PeriodEntity?

    @Query("SELECT * FROM period ORDER BY startDate ASC LIMIT 1")
    suspend fun earliest(): PeriodEntity?

    @Query("SELECT * FROM period WHERE :day BETWEEN startDate AND endDate LIMIT 1")
    suspend fun containing(day: LocalDate): PeriodEntity?

    @Query("SELECT * FROM period WHERE id = :id")
    fun observe(id: Long): Flow<PeriodEntity?>

    @Query("SELECT * FROM period ORDER BY startDate DESC")
    fun observeAll(): Flow<List<PeriodEntity>>

    @Query("SELECT COUNT(*) FROM period")
    suspend fun count(): Int
}

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("SELECT * FROM expense WHERE id = :id")
    suspend fun get(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expense WHERE periodId = :periodId ORDER BY occurredAt DESC")
    fun observeForPeriod(periodId: Long): Flow<List<ExpenseEntity>>
}

@Dao
interface IncomeDao {
    @Insert
    suspend fun insert(income: IncomeEntity): Long

    @Delete
    suspend fun delete(income: IncomeEntity)

    @Query("SELECT * FROM income WHERE periodId = :periodId ORDER BY occurredAt DESC")
    fun observeForPeriod(periodId: Long): Flow<List<IncomeEntity>>
}

@Dao
interface FixedCostDao {
    @Insert
    suspend fun insert(cost: FixedCostEntity): Long

    @Update
    suspend fun update(cost: FixedCostEntity)

    @Delete
    suspend fun delete(cost: FixedCostEntity)

    @Query("SELECT * FROM fixed_cost WHERE active = 1 ORDER BY name")
    fun observeActive(): Flow<List<FixedCostEntity>>

    @Query("SELECT * FROM fixed_cost WHERE active = 1 ORDER BY name")
    suspend fun getActive(): List<FixedCostEntity>
}

@Dao
interface CategoryDao {
    @Insert
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT * FROM category WHERE hidden = 0 ORDER BY sortOrder")
    fun observeVisible(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM category")
    suspend fun count(): Int
}
