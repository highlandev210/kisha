package com.example.kisha.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kisha.allowance.AllowanceEngine
import com.example.kisha.allowance.DateRange
import com.example.kisha.allowance.DayAllowance
import com.example.kisha.allowance.localDateIn
import com.example.kisha.data.BudgetRepository
import com.example.kisha.data.ExpenseRepository
import com.example.kisha.data.TimeSource
import com.example.kisha.data.db.ExpenseEntity
import com.example.kisha.data.range
import com.example.kisha.data.toBudget
import com.example.kisha.data.toEntry
import com.example.kisha.ui.common.ExpenseRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(
        val allowance: DayAllowance,
        val period: DateRange,
        /** Share of the period elapsed, including today: 1/30 on day 1 of a 30-day period. */
        val elapsedFraction: Float,
        val todayExpenses: List<ExpenseRow>,
    ) : HomeUiState
}

class HomeViewModel(
    private val budget: BudgetRepository,
    private val expenses: ExpenseRepository,
    today: Flow<LocalDate>,
    private val time: TimeSource,
) : ViewModel() {
    /** Derived on every change, never stored (rules 3 and 7). */
    val state: StateFlow<HomeUiState> = today
        .flatMapLatest { day ->
            flow { emit(budget.ensureCurrentPeriod(day)) }
                .filterNotNull()
                .flatMapLatest { current ->
                    combine(
                        budget.observePeriod(current.id).filterNotNull(),
                        expenses.observeForPeriod(current.id),
                        budget.observeIncome(current.id),
                        expenses.categories,
                    ) { period, periodExpenses, incomes, categories ->
                        val zone = time.zone()
                        val allowance = AllowanceEngine.dayAllowance(
                            period.toBudget(),
                            periodExpenses.filter { it.affectsAllowance }.map { it.toEntry(zone) },
                            incomes.map { it.toEntry(zone) },
                            day,
                        )
                        val byId = categories.associateBy { it.id }
                        val range = period.range()
                        HomeUiState.Ready(
                            allowance = allowance,
                            period = range,
                            elapsedFraction = (ChronoUnit.DAYS.between(range.start, allowance.day) + 1).toFloat() /
                                range.lengthInDays,
                            todayExpenses = periodExpenses
                                .filter { it.occurredAt.localDateIn(zone) == allowance.day }
                                .map { ExpenseRow(it, it.categoryId?.let(byId::get)) },
                        )
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun delete(expense: ExpenseEntity) {
        viewModelScope.launch { expenses.delete(expense) }
    }

    /** Undo for [delete]. Takes the expense itself so each snackbar restores the row it announced. */
    fun restore(expense: ExpenseEntity) {
        viewModelScope.launch { expenses.restore(expense) }
    }
}
