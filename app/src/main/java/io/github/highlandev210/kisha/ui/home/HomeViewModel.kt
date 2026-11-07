package io.github.highlandev210.kisha.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.highlandev210.kisha.allowance.AllowanceEngine
import io.github.highlandev210.kisha.allowance.DateRange
import io.github.highlandev210.kisha.allowance.DayAllowance
import io.github.highlandev210.kisha.allowance.Pocket
import io.github.highlandev210.kisha.allowance.PocketEngine
import io.github.highlandev210.kisha.allowance.localDateIn
import io.github.highlandev210.kisha.data.BudgetRepository
import io.github.highlandev210.kisha.data.ExpenseRepository
import io.github.highlandev210.kisha.data.TimeSource
import io.github.highlandev210.kisha.data.db.ExpenseEntity
import io.github.highlandev210.kisha.data.range
import io.github.highlandev210.kisha.data.toBudget
import io.github.highlandev210.kisha.data.toEntry
import io.github.highlandev210.kisha.ui.common.ExpenseRow
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

/** Finished days the user hasn't watched drop into the pocket yet, played as one coin. */
data class PocketCelebration(val throughDay: LocalDate, val deltaPaise: Long)

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(
        val allowance: DayAllowance,
        val period: DateRange,
        /** Share of the period elapsed, including today: 1/30 on day 1 of a 30-day period. */
        val elapsedFraction: Float,
        val todayExpenses: List<ExpenseRow>,
        val pocket: Pocket,
        /** Non-null once per new finished day (or run of days) until [HomeViewModel.markCelebrated]. */
        val celebration: PocketCelebration?,
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
                        budget.config.filterNotNull(),
                    ) { period, periodExpenses, incomes, categories, config ->
                        val zone = time.zone()
                        val budgetSnapshot = period.toBudget()
                        val entries = periodExpenses.filter { it.affectsAllowance }.map { it.toEntry(zone) }
                        val incomeEntries = incomes.map { it.toEntry(zone) }
                        val allowance = AllowanceEngine.dayAllowance(budgetSnapshot, entries, incomeEntries, day)
                        val pocket = PocketEngine.pocket(
                            budgetSnapshot, entries, incomeEntries, day, countFrom = config.setupDate ?: period.startDate,
                        )
                        val unseen = pocket.days.filter { config.pocketShownThrough == null || it.day.isAfter(config.pocketShownThrough) }
                        val byId = categories.associateBy { it.id }
                        val range = period.range()
                        HomeUiState.Ready(
                            allowance = allowance,
                            period = range,
                            elapsedFraction = (ChronoUnit.DAYS.between(range.start, allowance.day) + 1).toFloat() /
                                range.lengthInDays,
                            todayExpenses = periodExpenses
                                .filter { range.clamp(it.occurredAt.localDateIn(zone)) == allowance.day }
                                .map { ExpenseRow(it, it.categoryId?.let(byId::get)) },
                            pocket = pocket,
                            // The real change in the pocket since the last coin (it never dips below zero,
                            // so this can differ from the sum of the days' deltas).
                            celebration = unseen.takeIf { it.isNotEmpty() }?.let {
                                PocketCelebration(it.last().day, pocket.amountPaise - pocket.amountThrough(config.pocketShownThrough))
                            },
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

    /** Called when the coin animation for [celebration] has played (or was skipped). */
    fun markCelebrated(celebration: PocketCelebration) {
        viewModelScope.launch { budget.markPocketShown(celebration.throughDay) }
    }
}
