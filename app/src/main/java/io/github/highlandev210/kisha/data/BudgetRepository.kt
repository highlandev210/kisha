package io.github.highlandev210.kisha.data

import androidx.room.withTransaction
import io.github.highlandev210.kisha.allowance.CycleStart
import io.github.highlandev210.kisha.allowance.Cycles
import io.github.highlandev210.kisha.data.db.BudgetConfigEntity
import io.github.highlandev210.kisha.data.db.CategoryEntity
import io.github.highlandev210.kisha.data.db.FixedCostEntity
import io.github.highlandev210.kisha.data.db.IncomeEntity
import io.github.highlandev210.kisha.data.db.KishaDatabase
import io.github.highlandev210.kisha.data.db.PeriodEntity
import io.github.highlandev210.kisha.data.db.PeriodType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

data class NewFixedCost(val name: String, val amountPaise: Long, val dueDay: Int?)

data class SetupInput(
    val periodType: PeriodType,
    val cycleStart: CycleStart,
    val baseIncomePaise: Long,
    val fixedCosts: List<NewFixedCost>,
)

data class DefaultCategory(val name: String, val icon: String)

/** Budget configuration, periods (including automatic rollover), fixed costs and extra income. */
class BudgetRepository(private val db: KishaDatabase, private val time: TimeSource) {
    private val configDao = db.budgetConfigDao()
    private val periodDao = db.periodDao()
    private val fixedCostDao = db.fixedCostDao()
    private val incomeDao = db.incomeDao()
    private val rolloverLock = Mutex()

    val config: Flow<BudgetConfigEntity?> = configDao.observe()
    val periods: Flow<List<PeriodEntity>> = periodDao.observeAll()
    val fixedCosts: Flow<List<FixedCostEntity>> = fixedCostDao.observeActive()

    fun observePeriod(id: Long): Flow<PeriodEntity?> = periodDao.observe(id)

    fun observeIncome(periodId: Long): Flow<List<IncomeEntity>> = incomeDao.observeForPeriod(periodId)

    suspend fun completeSetup(input: SetupInput, categories: List<DefaultCategory>, today: LocalDate) {
        val rule = if (input.periodType == PeriodType.MONTH) CycleStart.DayOfMonth(1) else input.cycleStart
        db.withTransaction {
            configDao.upsert(
                BudgetConfigEntity(
                    periodType = input.periodType,
                    cycleStartDay = rule.stored,
                    baseIncome = input.baseIncomePaise,
                    setupDate = today,
                ),
            )
            input.fixedCosts.forEach {
                fixedCostDao.insert(FixedCostEntity(name = it.name, amountPaise = it.amountPaise, dueDay = it.dueDay))
            }
            db.categoryDao().insertAll(
                categories.mapIndexed { index, c ->
                    CategoryEntity(name = c.name, icon = c.icon, sortOrder = index, isDefault = true)
                },
            )
            val range = Cycles.periodContaining(today, rule)
            periodDao.insert(
                PeriodEntity(
                    startDate = range.start,
                    endDate = range.endInclusive,
                    income = input.baseIncomePaise,
                    fixedTotal = input.fixedCosts.sumOf { it.amountPaise },
                    savingsTarget = 0,
                ),
            )
        }
    }

    /**
     * Makes sure a period containing [today] exists, creating every missing period since the latest
     * one (BP-1). Returns the period to show: the one containing [today], or the earliest period when
     * the clock has moved before it. Null before setup.
     */
    suspend fun ensureCurrentPeriod(today: LocalDate): PeriodEntity? = rolloverLock.withLock {
        db.withTransaction {
            val config = configDao.get() ?: return@withTransaction null
            val latest = periodDao.latest() ?: return@withTransaction null
            if (today.isAfter(latest.endDate)) {
                val rule = CycleStart.fromStored(config.cycleStartDay)
                val ranges = Cycles.periodsAfter(latest.endDate, today, rule)
                val fixedTotal = fixedCostDao.getActive().sumOf { it.amountPaise }
                val closedAt = time.now()
                periodDao.update(latest.copy(closedAt = latest.closedAt ?: closedAt))
                ranges.forEachIndexed { index, range ->
                    periodDao.insert(
                        PeriodEntity(
                            startDate = range.start,
                            endDate = range.endInclusive,
                            income = config.baseIncome,
                            fixedTotal = fixedTotal,
                            savingsTarget = config.savingsTarget,
                            closedAt = if (index < ranges.lastIndex) closedAt else null,
                        ),
                    )
                }
            }
            periodDao.containing(today) ?: periodDao.earliest()
        }
    }

    suspend fun firstPeriodStart(): LocalDate? = periodDao.earliest()?.startDate

    suspend fun addFixedCost(cost: NewFixedCost) = db.withTransaction {
        fixedCostDao.insert(FixedCostEntity(name = cost.name, amountPaise = cost.amountPaise, dueDay = cost.dueDay))
        refreshOpenPeriodFixedTotal()
    }

    suspend fun updateFixedCost(cost: FixedCostEntity) = db.withTransaction {
        fixedCostDao.update(cost)
        refreshOpenPeriodFixedTotal()
    }

    suspend fun deleteFixedCost(cost: FixedCostEntity) = db.withTransaction {
        fixedCostDao.delete(cost)
        refreshOpenPeriodFixedTotal()
    }

    suspend fun setBaseIncome(paise: Long) = db.withTransaction {
        val config = checkNotNull(configDao.get()) { "setup not complete" }
        configDao.upsert(config.copy(baseIncome = paise))
        periodDao.latest()?.let { periodDao.update(it.copy(income = paise)) }
    }

    suspend fun addIncome(amountPaise: Long, note: String) {
        val period = checkNotNull(periodDao.containing(time.today()) ?: periodDao.latest()) { "setup not complete" }
        incomeDao.insert(
            IncomeEntity(periodId = period.id, amountPaise = amountPaise, note = note, occurredAt = time.now()),
        )
    }

    suspend fun deleteIncome(income: IncomeEntity) = incomeDao.delete(income)

    /** Records that the pocket animation for [day] has played, so it shows once per finished day. */
    suspend fun markPocketShown(day: LocalDate) = db.withTransaction {
        val config = configDao.get() ?: return@withTransaction
        if (config.pocketShownThrough == null || day.isAfter(config.pocketShownThrough)) {
            configDao.upsert(config.copy(pocketShownThrough = day))
        }
    }

    suspend fun setCurrency(symbol: String, indianGrouping: Boolean) = db.withTransaction {
        val config = checkNotNull(configDao.get()) { "setup not complete" }
        configDao.upsert(config.copy(currency = symbol, indianGrouping = indianGrouping))
    }

    /** Decision 5: fixed-cost edits apply to the open (latest) period; closed periods keep their snapshot. */
    private suspend fun refreshOpenPeriodFixedTotal() {
        val open = periodDao.latest() ?: return
        periodDao.update(open.copy(fixedTotal = fixedCostDao.getActive().sumOf { it.amountPaise }))
    }
}
