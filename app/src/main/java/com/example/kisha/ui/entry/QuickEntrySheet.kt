package com.example.kisha.ui.entry

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kisha.R
import com.example.kisha.allowance.DateRange
import com.example.kisha.ui.TestTags
import com.example.kisha.ui.common.ConnectedChoice
import com.example.kisha.ui.common.categoryIcon
import com.example.kisha.ui.format.LocalMoneyFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** UI-4: quick entry in a modal bottom sheet. Shown while the view model has a target. */
@Composable
fun QuickEntrySheet(viewModel: EntryViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.target == null) return
    ModalBottomSheet(
        onDismissRequest = viewModel::close,
        // Hidden or fully expanded only, so the keypad is always on screen (EX-1).
        sheetState = rememberBottomSheetState(SheetValue.Hidden, setOf(SheetValue.Hidden, SheetValue.Expanded)),
    ) {
        EntryContent(state, viewModel)
    }
}

@Composable
private fun EntryContent(state: EntryUiState, viewModel: EntryViewModel) {
    val formatter = LocalMoneyFormatter.current
    var pickingDate by remember { mutableStateOf(false) }
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    val local = state.occurredAt.atZone(state.zone)

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.target == EntryTarget.NewExpense) {
            ConnectedChoice(
                options = listOf(
                    EntryMode.EXPENSE to stringResource(R.string.entry_mode_expense),
                    EntryMode.INCOME to stringResource(R.string.entry_mode_income),
                ),
                selected = state.mode,
                onSelect = viewModel::setMode,
            )
        }
        Text(
            formatter.symbol + state.amount.text.ifEmpty { "0" },
            style = MaterialTheme.typography.displayMediumEmphasized,
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (state.mode == EntryMode.EXPENSE) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.categories, key = { it.id }) { category ->
                    val selected = state.categoryId == category.id
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectCategory(if (selected) null else category.id) },
                        label = { Text(category.name) },
                        leadingIcon = {
                            Icon(categoryIcon(category.icon), contentDescription = null, Modifier.size(FilterChipDefaults.IconSize))
                        },
                    )
                }
            }
        }
        OutlinedTextField(
            value = state.note,
            onValueChange = viewModel::setNote,
            label = { Text(stringResource(R.string.entry_note_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.mode == EntryMode.EXPENSE) {
                val timeText = local.toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
                val label = if (local.toLocalDate() == state.selectableDates?.endInclusive) {
                    stringResource(R.string.entry_today_at, timeText)
                } else {
                    local.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
                }
                AssistChip(
                    onClick = { pickingDate = true },
                    label = { Text(label) },
                    leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = stringResource(R.string.entry_date_time)) },
                )
            }
            if (state.target is EntryTarget.EditExpense) {
                TextButton(onClick = viewModel::deleteEditing, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.entry_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        // DP-2: Save sits directly above the keypad, where the thumb already is.
        Button(
            onClick = viewModel::save,
            enabled = state.amount.canSave,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(TestTags.ENTRY_SAVE),
        ) {
            Text(stringResource(R.string.entry_save), style = MaterialTheme.typography.titleMedium)
        }
        Keypad(onKey = viewModel::press)
    }

    if (pickingDate) {
        DateDialog(
            initial = local.toLocalDate(),
            range = state.selectableDates,
            onPicked = { pickingDate = false; pickedDate = it },
            onDismiss = { pickingDate = false },
        )
    }
    pickedDate?.let { date ->
        TimeDialog(
            initial = local.toLocalTime(),
            onPicked = { viewModel.setDateTime(date, it); pickedDate = null },
            onDismiss = { pickedDate = null },
        )
    }
}

@Composable
private fun DateDialog(initial: LocalDate, range: DateRange?, onPicked: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    fun LocalDate.utcMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.utcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val day = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return range == null || day in range
            }
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = pickerState.selectedDateMillis
                if (millis == null) onDismiss() else onPicked(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
            }) { Text(stringResource(R.string.dialog_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun TimeDialog(initial: LocalTime, onPicked: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val pickerState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPicked(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
        text = { TimePicker(state = pickerState) },
    )
}
