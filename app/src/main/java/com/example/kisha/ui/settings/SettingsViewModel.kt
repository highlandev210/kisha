package com.example.kisha.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kisha.allowance.Money
import com.example.kisha.data.BudgetRepository
import com.example.kisha.data.db.BudgetConfigEntity
import com.example.kisha.data.db.FixedCostEntity
import com.example.kisha.data.db.IncomeEntity
import com.example.kisha.data.db.PeriodEntity
import com.example.kisha.ui.onboarding.parseFixedCost
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

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Ready(
        val config: BudgetConfigEntity,
        val period: PeriodEntity,
        val fixedCosts: List<FixedCostEntity>,
        val extraIncome: List<IncomeEntity>,
    ) : SettingsUiState
}

class SettingsViewModel(private val budget: BudgetRepository, today: Flow<LocalDate>) : ViewModel() {
    val state: StateFlow<SettingsUiState> = today
        .flatMapLatest { day ->
            flow { emit(budget.ensureCurrentPeriod(day)) }
                .filterNotNull()
                .flatMapLatest { current ->
                    combine(
                        budget.config.filterNotNull(),
                        budget.observePeriod(current.id).filterNotNull(),
                        budget.fixedCosts,
                        budget.observeIncome(current.id),
                    ) { config, period, fixed, income -> SettingsUiState.Ready(config, period, fixed, income) }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState.Loading)

    /** IN-2. False (and nothing written) unless the amount is above zero. */
    fun setBaseIncome(text: String): Boolean {
        val paise = Money.parsePaise(text)?.takeIf { it > 0 } ?: return false
        viewModelScope.launch { budget.setBaseIncome(paise) }
        return true
    }

    /** FC-1/FC-2. False (and nothing written) unless the name is set, amount > 0, due day empty or 1–31. */
    fun addFixedCost(name: String, amountText: String, dueDayText: String): Boolean {
        val cost = parseFixedCost(name, amountText, dueDayText) ?: return false
        viewModelScope.launch { budget.addFixedCost(cost) }
        return true
    }

    fun updateFixedCost(cost: FixedCostEntity, name: String, amountText: String, dueDayText: String): Boolean {
        val parsed = parseFixedCost(name, amountText, dueDayText) ?: return false
        viewModelScope.launch {
            budget.updateFixedCost(cost.copy(name = parsed.name, amountPaise = parsed.amountPaise, dueDay = parsed.dueDay))
        }
        return true
    }

    fun deleteFixedCost(cost: FixedCostEntity) {
        viewModelScope.launch { budget.deleteFixedCost(cost) }
    }

    fun deleteIncome(income: IncomeEntity) {
        viewModelScope.launch { budget.deleteIncome(income) }
    }

    /** ST-1. The symbol is trimmed and must be 1–4 characters. */
    fun setCurrency(symbol: String, indianGrouping: Boolean): Boolean {
        val trimmed = symbol.trim().takeIf { it.length in 1..4 } ?: return false
        viewModelScope.launch { budget.setCurrency(trimmed, indianGrouping) }
        return true
    }
}
