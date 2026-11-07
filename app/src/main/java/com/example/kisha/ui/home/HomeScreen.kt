package com.example.kisha.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kisha.R
import com.example.kisha.allowance.DateRange
import com.example.kisha.allowance.DayAllowance
import com.example.kisha.data.db.ExpenseEntity
import com.example.kisha.ui.TestTags
import com.example.kisha.ui.common.ExpenseListItem
import com.example.kisha.ui.common.ExpenseRow
import com.example.kisha.ui.common.StatusLabel
import com.example.kisha.ui.common.statusColor
import com.example.kisha.ui.format.LocalMoneyFormatter
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(viewModel: HomeViewModel, onExpenseClick: (Long) -> Unit, snackbarHostState: SnackbarHostState) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.expense_deleted)
    val undoLabel = stringResource(R.string.undo)

    when (val s = state) {
        HomeUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        is HomeUiState.Ready -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Bottom padding keeps the last row clear of the FAB.
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { AllowanceHero(s.allowance) }
            item { PeriodBlock(s.allowance, s.period, s.elapsedFraction) }
            item {
                TodayBlock(
                    rows = s.todayExpenses,
                    onExpenseClick = onExpenseClick,
                    onDelete = { expense ->
                        viewModel.delete(expense)
                        // One snackbar at a time, and its Undo restores exactly the expense it announced.
                        snackbarHostState.currentSnackbarData?.dismiss()
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = deletedMessage,
                                actionLabel = undoLabel,
                                duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed) viewModel.restore(expense)
                        }
                    },
                )
            }
        }
    }
}

/** HM-1/HM-2, UI-3, DP-7: the dominant number, coloured by state, sprung into place on change. */
@Composable
private fun AllowanceHero(allowance: DayAllowance) {
    val formatter = LocalMoneyFormatter.current
    val color = statusColor(allowance.status)
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.home_today_label), style = MaterialTheme.typography.titleMedium)
        AnimatedContent(
            targetState = formatter.formatWhole(allowance.remainingPaise),
            transitionSpec = {
                (slideInVertically(spatial) { it / 2 } + fadeIn(effects)) togetherWith
                    (slideOutVertically(spatial) { -it / 2 } + fadeOut(effects))
            },
            label = "allowance",
        ) { text ->
            Text(
                text,
                style = MaterialTheme.typography.displayLargeEmphasized,
                color = color,
                modifier = Modifier.testTag(TestTags.ALLOWANCE),
            )
        }
        StatusLabel(allowance, MaterialTheme.typography.titleMediumEmphasized)
    }
}

private val dayMonth = DateTimeFormatter.ofPattern("d MMM")

/** HM-3, UI-3: money and days left, with the wavy indicator for how far through the period we are. */
@Composable
private fun PeriodBlock(allowance: DayAllowance, period: DateRange, elapsedFraction: Float) {
    val formatter = LocalMoneyFormatter.current
    val progressLabel = stringResource(R.string.home_period_progress)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.home_period_left, formatter.format(allowance.periodLeftPaise)),
                style = MaterialTheme.typography.titleLarge,
            )
            LinearWavyProgressIndicator(
                progress = { elapsedFraction },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = progressLabel },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.home_period_range, period.start.format(dayMonth), period.endInclusive.format(dayMonth)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    pluralStringResource(R.plurals.home_days_left, allowance.daysLeft, allowance.daysLeft),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** HM-4: today's expenses, newest first, swipe (or the TalkBack action) to delete. */
@Composable
private fun TodayBlock(rows: List<ExpenseRow>, onExpenseClick: (Long) -> Unit, onDelete: (ExpenseEntity) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                stringResource(R.string.home_spent_today),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (rows.isEmpty()) {
                Text(
                    stringResource(R.string.home_no_expenses),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            rows.forEach { row ->
                key(row.expense.id) {
                    SwipeToDeleteRow(row, onClick = { onExpenseClick(row.expense.id) }, onDelete = { onDelete(row.expense) })
                }
            }
        }
    }
}

@Composable
private fun SwipeToDeleteRow(row: ExpenseRow, onClick: () -> Unit, onDelete: () -> Unit) {
    val deleteLabel = stringResource(R.string.delete)
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = { if (it == SwipeToDismissBoxValue.EndToStart) onDelete() },
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        },
        modifier = Modifier.semantics {
            customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
        },
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            ExpenseListItem(row, onClick = onClick)
        }
    }
}
