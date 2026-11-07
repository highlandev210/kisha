package io.github.highlandev210.kisha.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.highlandev210.kisha.R
import io.github.highlandev210.kisha.data.db.FixedCostEntity
import io.github.highlandev210.kisha.data.db.IncomeEntity
import io.github.highlandev210.kisha.ui.common.ConnectedChoice
import io.github.highlandev210.kisha.ui.entry.AmountInput
import io.github.highlandev210.kisha.ui.format.LocalMoneyFormatter
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayMonth = DateTimeFormatter.ofPattern("d MMM")

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        SettingsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        is SettingsUiState.Ready -> Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IncomeSection(s, viewModel)
            FixedCostsSection(s.fixedCosts, viewModel)
            CurrencySection(s.config.currency, s.config.indianGrouping, viewModel::setCurrency)
        }
    }
}

@Composable
private fun Section(title: Int, content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}

/** IN-2 base income and the period's extra income (IN-1, logged from quick entry). */
@Composable
private fun IncomeSection(state: SettingsUiState.Ready, viewModel: SettingsViewModel) {
    val formatter = LocalMoneyFormatter.current
    var income by rememberSaveable(state.period.income) { mutableStateOf(AmountInput.fromPaise(state.period.income).text) }
    Section(R.string.settings_income_title) {
        OutlinedTextField(
            value = income,
            onValueChange = { income = it },
            label = { Text(stringResource(R.string.settings_income_label)) },
            prefix = { Text(formatter.symbol) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { viewModel.setBaseIncome(income) },
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.heightIn(min = 56.dp),
        ) { Text(stringResource(R.string.settings_save)) }
        Text(stringResource(R.string.settings_extra_income), style = MaterialTheme.typography.titleMedium)
        if (state.extraIncome.isEmpty()) {
            Text(stringResource(R.string.settings_no_extra_income), style = MaterialTheme.typography.bodyMedium)
        }
        var confirming by remember { mutableStateOf<IncomeEntity?>(null) }
        val incomeName = stringResource(R.string.entry_mode_income)
        state.extraIncome.forEach { item ->
            val name = item.note.ifBlank { incomeName }
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                supportingContent = { Text(item.occurredAt.atZone(ZoneId.systemDefault()).format(dayMonth)) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatter.format(item.amountPaise))
                        IconButton(onClick = { confirming = item }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.settings_delete_named, name))
                        }
                    }
                },
            ) { Text(name) }
        }
        confirming?.let { item ->
            ConfirmDelete(
                name = item.note.ifBlank { incomeName },
                body = R.string.settings_confirm_delete_income,
                onConfirm = { viewModel.deleteIncome(item); confirming = null },
                onDismiss = { confirming = null },
            )
        }
    }
}

/** Deleting money that's set aside (or extra income) silently changes the daily number, so ask first. */
@Composable
private fun ConfirmDelete(name: String, body: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_confirm_delete_title, name)) },
        text = { Text(stringResource(body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

/** FC-1/FC-2: add, edit and delete fixed costs; edits apply to this period straight away. */
@Composable
private fun FixedCostsSection(costs: List<FixedCostEntity>, viewModel: SettingsViewModel) {
    val formatter = LocalMoneyFormatter.current
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var confirming by remember { mutableStateOf<FixedCostEntity?>(null) }
    confirming?.let { cost ->
        ConfirmDelete(
            name = cost.name,
            body = R.string.settings_confirm_delete_fixed,
            onConfirm = { viewModel.deleteFixedCost(cost); confirming = null },
            onDismiss = { confirming = null },
        )
    }
    Section(R.string.settings_fixed_title) {
        if (costs.isEmpty()) {
            Text(stringResource(R.string.settings_fixed_empty), style = MaterialTheme.typography.bodyMedium)
        }
        costs.forEach { cost ->
            ListItem(
                modifier = Modifier.clickable { editing = cost.id },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                supportingContent = cost.dueDay?.let { { Text(stringResource(R.string.fixed_due_on, it)) } },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatter.format(cost.amountPaise))
                        IconButton(onClick = { confirming = cost }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.settings_delete_named, cost.name))
                        }
                    }
                },
            ) { Text(cost.name) }
        }
        FilledTonalButton(
            onClick = { adding = true },
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.heightIn(min = 56.dp),
        ) { Text(stringResource(R.string.settings_fixed_add)) }
    }

    if (adding) {
        FixedCostDialog(
            title = R.string.settings_fixed_add,
            initial = null,
            onConfirm = { name, amount, due -> viewModel.addFixedCost(name, amount, due).also { if (it) adding = false } },
            onDismiss = { adding = false },
        )
    }
    costs.firstOrNull { it.id == editing }?.let { cost ->
        FixedCostDialog(
            title = R.string.settings_fixed_edit,
            initial = cost,
            onConfirm = { name, amount, due ->
                viewModel.updateFixedCost(cost, name, amount, due).also { if (it) editing = null }
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun FixedCostDialog(
    title: Int,
    initial: FixedCostEntity?,
    onConfirm: (name: String, amount: String, dueDay: String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var amount by rememberSaveable { mutableStateOf(initial?.let { AmountInput.fromPaise(it.amountPaise).text }.orEmpty()) }
    var dueDay by rememberSaveable { mutableStateOf(initial?.dueDay?.toString().orEmpty()) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.fixed_name_label)) },
                    singleLine = true,
                    isError = invalid,
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.fixed_amount_label)) },
                    prefix = { Text(LocalMoneyFormatter.current.symbol) },
                    singleLine = true,
                    isError = invalid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = dueDay,
                    onValueChange = { dueDay = it.filter(Char::isDigit).take(2) },
                    label = { Text(stringResource(R.string.fixed_due_day_label)) },
                    singleLine = true,
                    isError = invalid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { invalid = !onConfirm(name, amount, dueDay) }) { Text(stringResource(R.string.settings_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

/** ST-1: currency symbol and grouping style. */
@Composable
private fun CurrencySection(symbol: String, indianGrouping: Boolean, onSave: (String, Boolean) -> Boolean) {
    var text by rememberSaveable(symbol) { mutableStateOf(symbol) }
    var indian by rememberSaveable(indianGrouping) { mutableStateOf(indianGrouping) }
    Section(R.string.settings_currency_title) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(4) },
            label = { Text(stringResource(R.string.settings_currency_symbol)) },
            singleLine = true,
            isError = text.isBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        ConnectedChoice(
            options = listOf(
                true to stringResource(R.string.grouping_indian),
                false to stringResource(R.string.grouping_international),
            ),
            selected = indian,
            onSelect = { indian = it },
        )
        Button(
            onClick = { onSave(text, indian) },
            enabled = text.isNotBlank(),
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.heightIn(min = 56.dp),
        ) { Text(stringResource(R.string.settings_save)) }
    }
}
