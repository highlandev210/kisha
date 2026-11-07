package io.github.highlandev210.kisha.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.highlandev210.kisha.AppContainer
import io.github.highlandev210.kisha.R
import io.github.highlandev210.kisha.data.db.PeriodType
import io.github.highlandev210.kisha.ui.TestTags
import io.github.highlandev210.kisha.ui.common.ConnectedChoice
import io.github.highlandev210.kisha.ui.format.LocalMoneyFormatter
import io.github.highlandev210.kisha.ui.theme.KishaTheme

@Composable
fun OnboardingScreen(container: AppContainer) {
    val context = LocalContext.current
    val viewModel = viewModel {
        OnboardingViewModel(container.budgetRepository, container.time, defaultCategories(context))
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    // System back steps through setup instead of leaving the app and losing what was entered.
    BackHandler(enabled = state.step > 0, onBack = viewModel::back)

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.step > 0) {
                    TextButton(onClick = viewModel::back, modifier = Modifier.heightIn(min = 56.dp)) {
                        Text(stringResource(R.string.onboarding_back))
                    }
                }
                Spacer(Modifier.weight(1f))
                val label = when {
                    state.step == 3 -> R.string.onboarding_start
                    state.step == 2 && state.fixedCosts.isEmpty() && state.draftBlank -> R.string.onboarding_skip
                    else -> R.string.onboarding_next
                }
                Button(
                    onClick = if (state.step == 3) viewModel::finish else viewModel::next,
                    enabled = state.canAdvance,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.heightIn(min = 56.dp),
                ) {
                    Text(stringResource(label))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.onboarding_step, state.step + 1, OnboardingState.STEPS),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            when (state.step) {
                0 -> IncomeStep(state.incomeText, viewModel::setIncome)
                1 -> CycleStep(state, viewModel)
                2 -> FixedCostsStep(state, viewModel)
                else -> PreviewStep(state)
            }
        }
    }
}

@Composable
private fun StepHeader(title: Int, body: Int? = null) {
    Text(stringResource(title), style = MaterialTheme.typography.headlineMediumEmphasized)
    if (body != null) {
        Text(stringResource(body), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun IncomeStep(income: String, onIncome: (String) -> Unit) {
    StepHeader(R.string.onboarding_income_title, R.string.onboarding_income_body)
    OutlinedTextField(
        value = income,
        onValueChange = onIncome,
        label = { Text(stringResource(R.string.onboarding_income_label)) },
        prefix = { Text(LocalMoneyFormatter.current.symbol) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().testTag(TestTags.INCOME_FIELD),
    )
}

@Composable
private fun CycleStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    StepHeader(R.string.onboarding_cycle_title, R.string.onboarding_cycle_body)
    ConnectedChoice(
        options = listOf(
            PeriodType.MONTH to stringResource(R.string.cycle_calendar_month),
            PeriodType.CUSTOM to stringResource(R.string.cycle_payday),
        ),
        selected = state.periodType,
        onSelect = viewModel::setPeriodType,
    )
    if (state.periodType == PeriodType.CUSTOM) {
        ConnectedChoice(
            options = listOf(
                PaydayMode.DAY_OF_MONTH to stringResource(R.string.cycle_day_of_month),
                PaydayMode.LAST_WORKING_DAY to stringResource(R.string.cycle_last_working_day),
            ),
            selected = state.paydayMode,
            onSelect = viewModel::setPaydayMode,
        )
        if (state.paydayMode == PaydayMode.DAY_OF_MONTH) {
            OutlinedTextField(
                value = state.paydayText,
                onValueChange = viewModel::setPaydayText,
                label = { Text(stringResource(R.string.cycle_day_label)) },
                singleLine = true,
                isError = state.cycleStart == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FixedCostsStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    val formatter = LocalMoneyFormatter.current
    StepHeader(R.string.onboarding_fixed_title, R.string.onboarding_fixed_body)
    state.fixedCosts.forEachIndexed { index, cost ->
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large) {
            ListItem(
                supportingContent = cost.dueDay?.let { { Text(stringResource(R.string.fixed_due_on, it)) } },
                trailingContent = {
                    Row {
                        Text(formatter.format(cost.amountPaise), modifier = Modifier.padding(top = 12.dp))
                        IconButton(onClick = { viewModel.removeFixedCost(index) }) {
                            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.fixed_remove, cost.name))
                        }
                    }
                },
            ) {
                Text(cost.name)
            }
        }
    }
    FixedCostForm(state, viewModel)
}

/** Name, amount and optional due day. State lives in the view model so Next can add a typed-in cost. */
@Composable
private fun FixedCostForm(state: OnboardingState, viewModel: OnboardingViewModel) {
    OutlinedTextField(
        value = state.draftName,
        onValueChange = viewModel::setDraftName,
        label = { Text(stringResource(R.string.fixed_name_label)) },
        singleLine = true,
        isError = state.draftError,
        modifier = Modifier.fillMaxWidth().testTag(TestTags.FIXED_NAME_FIELD),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.draftAmount,
            onValueChange = viewModel::setDraftAmount,
            label = { Text(stringResource(R.string.fixed_amount_label)) },
            prefix = { Text(LocalMoneyFormatter.current.symbol) },
            singleLine = true,
            isError = state.draftError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag(TestTags.FIXED_AMOUNT_FIELD),
        )
        OutlinedTextField(
            value = state.draftDueDay,
            onValueChange = viewModel::setDraftDueDay,
            label = { Text(stringResource(R.string.fixed_due_day_label)) },
            singleLine = true,
            isError = state.draftError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
    }
    if (state.draftError) {
        Text(
            stringResource(R.string.fixed_invalid),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    FilledTonalButton(
        onClick = viewModel::addDraft,
        shapes = ButtonDefaults.shapes(),
        modifier = Modifier.heightIn(min = 56.dp),
    ) {
        Text(stringResource(R.string.fixed_add))
    }
}

@Composable
private fun PreviewStep(state: OnboardingState) {
    val preview = state.preview ?: return
    val formatter = LocalMoneyFormatter.current
    // A first taste of Home's hero: status colour, the giant number, the burst.
    val status = KishaTheme.colors.status(preview.status)
    StepHeader(R.string.onboarding_preview_title)
    Surface(
        color = status.container,
        contentColor = status.content,
        shape = MaterialTheme.shapes.extraLargeIncreased.copy(bottomStart = MaterialTheme.shapes.small.bottomStart),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 70.dp, y = (-40).dp)
                    .size(200.dp)
                    .clip(MaterialShapes.Sunny.toShape())
                    .background(status.content.copy(alpha = 0.13f)),
            )
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.onboarding_preview_label), style = MaterialTheme.typography.titleMedium)
                Text(
                    formatter.formatWhole(preview.remainingPaise),
                    style = KishaTheme.heroNumber,
                    maxLines = 1,
                    autoSize = KishaTheme.heroAutoSize,
                    modifier = Modifier.testTag(TestTags.PREVIEW_ALLOWANCE),
                )
                Text(
                    pluralStringResource(R.plurals.onboarding_preview_days, preview.daysLeft, preview.daysLeft),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
