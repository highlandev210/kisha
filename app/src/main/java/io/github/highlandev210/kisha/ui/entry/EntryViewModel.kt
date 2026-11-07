package io.github.highlandev210.kisha.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.highlandev210.kisha.allowance.DateRange
import io.github.highlandev210.kisha.data.BudgetRepository
import io.github.highlandev210.kisha.data.ExpenseRepository
import io.github.highlandev210.kisha.data.TimeSource
import io.github.highlandev210.kisha.data.db.CategoryEntity
import io.github.highlandev210.kisha.data.db.ExpenseEntity
import io.github.highlandev210.kisha.data.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

sealed interface EntryTarget {
    data object NewExpense : EntryTarget
    data class EditExpense(val id: Long) : EntryTarget
}

enum class EntryMode { EXPENSE, INCOME }

data class EntryUiState(
    /** Null while the sheet is closed. */
    val target: EntryTarget? = null,
    val mode: EntryMode = EntryMode.EXPENSE,
    val amount: AmountInput = AmountInput(),
    val categoryId: Long? = null,
    val note: String = "",
    val occurredAt: Instant = Instant.EPOCH,
    val zone: ZoneId = ZoneId.systemDefault(),
    val categories: List<CategoryEntity> = emptyList(),
    /** EX-2 date picker bounds: first period start → today. */
    val selectableDates: DateRange? = null,
    /** True once the user picked a date/time; until then a new expense is stamped when saved. */
    val dateTouched: Boolean = false,
)

class EntryViewModel(
    private val expenses: ExpenseRepository,
    private val budget: BudgetRepository,
    private val time: TimeSource,
) : ViewModel() {
    private val _state = MutableStateFlow(EntryUiState())
    val state: StateFlow<EntryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            expenses.categories.collect { categories -> _state.update { it.copy(categories = categories) } }
        }
    }

    fun open(target: EntryTarget) {
        _state.update {
            EntryUiState(target = target, occurredAt = time.now(), zone = time.zone(), categories = it.categories)
        }
        viewModelScope.launch {
            val today = time.today()
            val first = budget.firstPeriodStart() ?: today
            _state.update { it.copy(selectableDates = DateRange(minOf(first, today), today)) }
            if (target is EntryTarget.EditExpense) {
                val expense = expenses.get(target.id) ?: return@launch close()
                _state.update {
                    it.copy(
                        amount = AmountInput.fromPaise(expense.amountPaise),
                        categoryId = expense.categoryId,
                        note = expense.note,
                        occurredAt = expense.occurredAt,
                    )
                }
            }
        }
    }

    fun close() = _state.update { it.copy(target = null) }

    fun press(key: Key) = _state.update { it.copy(amount = it.amount.press(key)) }

    fun setMode(mode: EntryMode) = _state.update { it.copy(mode = mode) }

    fun selectCategory(id: Long?) = _state.update { it.copy(categoryId = id) }

    fun setNote(note: String) = _state.update { it.copy(note = note) }

    fun setDateTime(date: LocalDate, time: LocalTime) = _state.update {
        it.copy(occurredAt = date.atTime(time).atZone(it.zone).toInstant(), dateTouched = true)
    }

    fun save() {
        val s = _state.value
        val target = s.target ?: return
        if (!s.amount.canSave) return
        close()
        viewModelScope.launch {
            val note = s.note.trim()
            when {
                s.mode == EntryMode.INCOME -> budget.addIncome(s.amount.paise, note)
                target is EntryTarget.EditExpense -> expenses.get(target.id)?.let {
                    expenses.update(
                        it.copy(amountPaise = s.amount.paise, categoryId = s.categoryId, note = note, occurredAt = s.occurredAt),
                    )
                }
                // A sheet left open across midnight saves to when Save was tapped, unless a time was picked.
                else -> expenses.add(s.amount.paise, s.categoryId, note, if (s.dateTouched) s.occurredAt else time.now())
            }
        }
    }

    /** Deletes the expense being edited and hands it to [onDeleted] so the caller can offer Undo. */
    fun deleteEditing(onDeleted: (ExpenseEntity) -> Unit = {}) {
        val target = _state.value.target as? EntryTarget.EditExpense ?: return
        close()
        viewModelScope.launch {
            expenses.get(target.id)?.let {
                expenses.delete(it)
                onDeleted(it)
            }
        }
    }

    /** Undo for [deleteEditing]. */
    fun restore(expense: ExpenseEntity) {
        viewModelScope.launch { expenses.restore(expense) }
    }
}
