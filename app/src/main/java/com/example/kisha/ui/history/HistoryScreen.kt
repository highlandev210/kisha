package com.example.kisha.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kisha.R
import com.example.kisha.ui.common.ExpenseListItem
import com.example.kisha.ui.common.StatusLabel
import com.example.kisha.ui.format.LocalMoneyFormatter
import java.time.format.DateTimeFormatter

private val dayFormat = DateTimeFormatter.ofPattern("EEE, d MMM")
private val rangeFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

/** HS-1/HS-2 and DP-4: a plain list of days, newest first, each with spent vs allowance. */
@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onExpenseClick: (Long) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        HistoryUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        is HistoryUiState.Ready -> LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                PeriodSwitcher(s, onPrevious = viewModel::previous, onNext = viewModel::next)
            }
            s.days.forEach { day ->
                item(key = "day-${day.allowance.day}") { DayHeader(day) }
                items(day.expenses, key = { it.expense.id }) { row ->
                    ExpenseListItem(row, onClick = { onExpenseClick(row.expense.id) })
                }
            }
        }
    }
}

@Composable
private fun PeriodSwitcher(state: HistoryUiState.Ready, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(onClick = onPrevious, enabled = state.hasOlder) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.history_previous))
        }
        Text(
            stringResource(
                R.string.home_period_range,
                state.selected.startDate.format(rangeFormat),
                state.selected.endDate.format(rangeFormat),
            ),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        FilledTonalIconButton(onClick = onNext, enabled = state.hasNewer) {
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = stringResource(R.string.history_next))
        }
    }
}

@Composable
private fun DayHeader(day: HistoryDay) {
    val formatter = LocalMoneyFormatter.current
    val a = day.allowance
    Column {
        HorizontalDivider()
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).semantics(mergeDescendants = true) { heading() },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a.day.format(dayFormat), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.history_spent_of, formatter.format(a.spentPaise), formatter.formatWhole(a.basePaise)),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            StatusLabel(a, MaterialTheme.typography.labelLarge)
        }
    }
}
