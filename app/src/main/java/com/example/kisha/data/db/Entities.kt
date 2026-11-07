package com.example.kisha.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

// All money columns are Long paise.

enum class PeriodType { MONTH, CUSTOM }

enum class AllowanceMode { SPREAD, CARRY }

/** Single row (id = 1) that drives each new period. */
@Entity(tableName = "budget_config")
data class BudgetConfigEntity(
    @PrimaryKey val id: Int = 1,
    val periodType: PeriodType,
    /** 1..31 = day of month; -1 = last working day. MONTH stores 1. */
    val cycleStartDay: Int,
    val baseIncome: Long,
    val savingsTarget: Long = 0,
    val allowanceMode: AllowanceMode = AllowanceMode.SPREAD,
    val currency: String = "₹",
    val indianGrouping: Boolean = true,
    /** The day setup finished; the pocket only counts days from here (schema v2). */
    val setupDate: LocalDate? = null,
    /** The last finished day whose pocket coin animation has played (schema v2). */
    val pocketShownThrough: LocalDate? = null,
)

/** One row per cycle: a snapshot, so history stays correct after settings change. */
@Entity(tableName = "period")
data class PeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val income: Long,
    val fixedTotal: Long,
    val savingsTarget: Long,
    val leftoverAction: String? = null,
    val closedAt: Instant? = null,
)

@Entity(
    tableName = "expense",
    foreignKeys = [
        ForeignKey(entity = PeriodEntity::class, parentColumns = ["id"], childColumns = ["periodId"]),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("occurredAt"), Index("periodId"), Index("categoryId")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val periodId: Long,
    val amountPaise: Long,
    val categoryId: Long?,
    val note: String = "",
    val occurredAt: Instant,
    val affectsAllowance: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "income",
    foreignKeys = [ForeignKey(entity = PeriodEntity::class, parentColumns = ["id"], childColumns = ["periodId"])],
    indices = [Index("periodId")],
)
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val periodId: Long,
    val amountPaise: Long,
    val note: String = "",
    val occurredAt: Instant,
    val recurring: Boolean = false,
)

/** Template copied into each period's fixedTotal. */
@Entity(tableName = "fixed_cost")
data class FixedCostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountPaise: Long,
    val dueDay: Int? = null,
    val active: Boolean = true,
    val paidInPeriodId: Long? = null,
)

@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Icon key, mapped to an ImageVector in the UI layer. */
    val icon: String,
    val colour: Int? = null,
    val sortOrder: Int,
    val hidden: Boolean = false,
    val isDefault: Boolean,
)
