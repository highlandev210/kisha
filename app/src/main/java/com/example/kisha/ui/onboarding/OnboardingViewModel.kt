package com.example.kisha.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kisha.R
import com.example.kisha.allowance.AllowanceEngine
import com.example.kisha.allowance.CycleStart
import com.example.kisha.allowance.Cycles
import com.example.kisha.allowance.DayAllowance
import com.example.kisha.allowance.Money
import com.example.kisha.allowance.PeriodBudget
import com.example.kisha.data.BudgetRepository
import com.example.kisha.data.DefaultCategory
import com.example.kisha.data.NewFixedCost
import com.example.kisha.data.SetupInput
import com.example.kisha.data.TimeSource
import com.example.kisha.data.db.PeriodType
import com.example.kisha.data.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PaydayMode { DAY_OF_MONTH, LAST_WORKING_DAY }

data class OnboardingState(
    val step: Int = 0,
    val incomeText: String = "",
    val periodType: PeriodType = PeriodType.MONTH,
    val paydayMode: PaydayMode = PaydayMode.DAY_OF_MONTH,
    val paydayText: String = "1",
    val fixedCosts: List<NewFixedCost> = emptyList(),
    /** The fixed-cost form on step 3, kept here so Next can add what was typed but not added. */
    val draftName: String = "",
    val draftAmount: String = "",
    val draftDueDay: String = "",
    val draftError: Boolean = false,
    val preview: DayAllowance? = null,
    val finishing: Boolean = false,
) {
    val incomePaise: Long get() = Money.parsePaise(incomeText) ?: 0

    val draftBlank: Boolean get() = draftName.isBlank() && draftAmount.isBlank() && draftDueDay.isBlank()

    /** Null while a payday cycle has an invalid day typed in. */
    val cycleStart: CycleStart?
        get() = when {
            periodType == PeriodType.MONTH -> CycleStart.DayOfMonth(1)
            paydayMode == PaydayMode.LAST_WORKING_DAY -> CycleStart.LastWorkingDay
            else -> paydayText.toIntOrNull()?.takeIf { it in 1..31 }?.let { CycleStart.DayOfMonth(it) }
        }

    val canAdvance: Boolean
        get() = when (step) {
            0 -> incomePaise > 0
            1 -> cycleStart != null
            3 -> !finishing
            else -> true
        }

    companion object {
        const val STEPS = 4
    }
}

class OnboardingViewModel(
    private val budget: BudgetRepository,
    private val time: TimeSource,
    private val defaultCategories: List<DefaultCategory>,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun setIncome(text: String) = _state.update { it.copy(incomeText = text) }

    fun setPeriodType(type: PeriodType) = _state.update { it.copy(periodType = type) }

    fun setPaydayMode(mode: PaydayMode) = _state.update { it.copy(paydayMode = mode) }

    fun setPaydayText(text: String) = _state.update { it.copy(paydayText = text.filter(Char::isDigit).take(2)) }

    fun setDraftName(text: String) = _state.update { it.copy(draftName = text, draftError = false) }

    fun setDraftAmount(text: String) = _state.update { it.copy(draftAmount = text, draftError = false) }

    fun setDraftDueDay(text: String) =
        _state.update { it.copy(draftDueDay = text.filter(Char::isDigit).take(2), draftError = false) }

    /** Adds the drafted fixed cost; on invalid input keeps the draft and flags the error instead. */
    fun addDraft() = _state.update(::withDraftAdded)

    fun removeFixedCost(index: Int) = _state.update {
        it.copy(fixedCosts = it.fixedCosts.filterIndexed { i, _ -> i != index })
    }

    fun next() = _state.update {
        when {
            !it.canAdvance || it.step >= OnboardingState.STEPS - 1 -> it
            it.step == 2 -> {
                // A filled-in form counts as "Add" — never drop what the user typed.
                val added = if (it.draftBlank) it else withDraftAdded(it)
                if (added.draftError) added else added.copy(step = 3, preview = preview(added))
            }
            else -> it.copy(step = it.step + 1)
        }
    }

    private fun withDraftAdded(s: OnboardingState): OnboardingState {
        val cost = parseFixedCost(s.draftName, s.draftAmount, s.draftDueDay) ?: return s.copy(draftError = true)
        return s.copy(fixedCosts = s.fixedCosts + cost, draftName = "", draftAmount = "", draftDueDay = "", draftError = false)
    }

    fun back() = _state.update { if (it.step > 0) it.copy(step = it.step - 1) else it }

    fun finish() {
        val s = _state.value
        val cycleStart = s.cycleStart ?: return
        if (s.finishing) return
        _state.update { it.copy(finishing = true) }
        viewModelScope.launch {
            budget.completeSetup(
                SetupInput(s.periodType, cycleStart, s.incomePaise, s.fixedCosts),
                defaultCategories,
                time.today(),
            )
        }
    }

    private fun preview(s: OnboardingState): DayAllowance {
        val today = time.today()
        val range = Cycles.periodContaining(today, checkNotNull(s.cycleStart))
        val budget = PeriodBudget(range, s.incomePaise, s.fixedCosts.sumOf { it.amountPaise }, 0)
        return AllowanceEngine.dayAllowance(budget, emptyList(), emptyList(), today)
    }
}

/** Shared validation for fixed costs (onboarding and Settings). */
fun parseFixedCost(name: String, amountText: String, dueDayText: String): NewFixedCost? {
    val trimmed = name.trim()
    val amount = Money.parsePaise(amountText) ?: return null
    val dueDay = if (dueDayText.isBlank()) null else dueDayText.trim().toIntOrNull()?.takeIf { it in 1..31 } ?: return null
    if (trimmed.isEmpty() || amount <= 0) return null
    return NewFixedCost(trimmed, amount, dueDay)
}

/** CT-1 defaults, named from string resources at setup time. */
fun defaultCategories(context: Context): List<DefaultCategory> = listOf(
    R.string.category_food to "restaurant",
    R.string.category_transport to "directions_bus",
    R.string.category_groceries to "local_grocery_store",
    R.string.category_shopping to "shopping_bag",
    R.string.category_bills to "receipt_long",
    R.string.category_health to "medical_services",
    R.string.category_entertainment to "movie",
    R.string.category_other to "category",
).map { (name, icon) -> DefaultCategory(context.getString(name), icon) }
