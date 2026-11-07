package com.example.kisha.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kisha.allowance.AllowanceEngine
import com.example.kisha.allowance.DayAllowance
import com.example.kisha.allowance.PocketEngine
import com.example.kisha.allowance.localDateIn
import com.example.kisha.data.BudgetRepository
import com.example.kisha.data.ExpenseRepository
import com.example.kisha.data.TimeSource
import com.example.kisha.data.db.CategoryEntity
import com.example.kisha.data.db.ExpenseEntity
import com.example.kisha.data.db.IncomeEntity
import com.example.kisha.data.db.PeriodEntity
import com.example.kisha.data.range
import com.example.kisha.data.toBudget
import com.example.kisha.data.toEntry
import com.example.kisha.ui.common.ExpenseRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HistoryDay(
    val allowance: DayAllowance,
    val expenses: List<ExpenseRow>,
    /** What this finished day added to (+) or took from (−) the pocket; null for today and days before setup. */
    val pocketDeltaPaise: Long?,
)

sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    data class Ready(
        /** Newest first. */
        val periods: List<PeriodEntity>,
        val selected: PeriodEntity,
        /** Newest first, from today (or the period's end) back to its start; days without spending included. */
        val days: List<HistoryDay>,
    ) : HistoryUiState {
        private val index get() = periods.indexOfFirst { it.id == selected.id }
        val hasOlder: Boolean get() = index in 0 until periods.lastIndex
        val hasNewer: Boolean get() = index > 0
    }
}

class HistoryViewModel(
    private val budget: BudgetRepository,
    private val expenses: ExpenseRepository,
    today: Flow<LocalDate>,
    private val time: TimeSource,
) : ViewModel() {
    /** Null means "the current period". */
    private val selectedId = MutableStateFlow<Long?>(null)

    val state: StateFlow<HistoryUiState> = today
        .flatMapLatest { day ->
            flow { emit(budget.ensureCurrentPeriod(day)) }
                .filterNotNull()
                .flatMapLatest { current ->
                    combine(budget.periods, selectedId) { periods, id ->
                        periods to (periods.firstOrNull { it.id == (id ?: current.id) } ?: current)
                    }.flatMapLatest { (periods, selected) ->
                        combine(
                            expenses.observeForPeriod(selected.id),
                            budget.observeIncome(selected.id),
                            expenses.categories,
                            budget.config.filterNotNull(),
                        ) { periodExpenses, incomes, categories, config ->
                            HistoryUiState.Ready(
                                periods,
                                selected,
                                buildDays(selected, periodExpenses, incomes, categories, day, config.setupDate ?: selected.startDate),
                            )
                        }
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)

    private fun buildDays(
        period: PeriodEntity,
        periodExpenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>,
        categories: List<CategoryEntity>,
        today: LocalDate,
        countFrom: LocalDate,
    ): List<HistoryDay> {
        val zone = time.zone()
        val range = period.range()
        val budgetSnapshot = period.toBudget()
        val entries = periodExpenses.filter { it.affectsAllowance }.map { it.toEntry(zone) }
        val incomeEntries = incomes.map { it.toEntry(zone) }
        val pocketByDay = PocketEngine.pocket(budgetSnapshot, entries, incomeEntries, today, countFrom)
            .days.associate { it.day to it.deltaPaise }
        val byId = categories.associateBy { it.id }
        val rowsByDay = periodExpenses.groupBy { range.clamp(it.occurredAt.localDateIn(zone)) }
        val last = range.clamp(minOf(today, range.endInclusive))
        return generateSequence(last) { it.minusDays(1) }
            .takeWhile { !it.isBefore(range.start) }
            .map { day ->
                HistoryDay(
                    allowance = AllowanceEngine.dayAllowance(budgetSnapshot, entries, incomeEntries, day),
                    expenses = rowsByDay[day].orEmpty().map { ExpenseRow(it, it.categoryId?.let(byId::get)) },
                    pocketDeltaPaise = pocketByDay[day],
                )
            }
            .toList()
    }

    fun selectPeriod(id: Long) {
        selectedId.value = id
    }

    /** Moves to the next older period, if any. */
    fun previous() = step(+1)

    /** Moves to the next newer period, if any. */
    fun next() = step(-1)

    private fun step(delta: Int) {
        val ready = state.value as? HistoryUiState.Ready ?: return
        val index = ready.periods.indexOfFirst { it.id == ready.selected.id }
        ready.periods.getOrNull(index + delta)?.let { selectPeriod(it.id) }
    }
}
