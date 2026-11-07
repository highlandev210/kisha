package com.example.kisha.ui.history

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kisha.R
import com.example.kisha.allowance.AllowanceStatus
import com.example.kisha.ui.common.ExpenseListItem
import com.example.kisha.ui.common.StatusLabel
import com.example.kisha.ui.format.LocalMoneyFormatter
import com.example.kisha.ui.theme.KishaTheme

private val dayFormat = java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM")
private val rangeFormat = java.time.format.DateTimeFormatter.ofPattern("d MMM")
private val yearFormat = java.time.format.DateTimeFormatter.ofPattern("yyyy")

/** HS-1/HS-2: one tile per day, newest first, coloured by how the day went and what it did to the pocket. */
@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onExpenseClick: (Long) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        HistoryUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        is HistoryUiState.Ready -> LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "switcher") { PeriodSwitcher(s, onPrevious = viewModel::previous, onNext = viewModel::next) }
            items(s.days, key = { "day-${it.allowance.day}" }) { day ->
                DayTile(
                    day = day,
                    isToday = day.pocketDeltaPaise == null && day == s.days.first(),
                    onExpenseClick = onExpenseClick,
                    modifier = Modifier.animateItem(placementSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)),
                )
            }
        }
    }
}

@Composable
private fun PeriodSwitcher(state: HistoryUiState.Ready, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(onClick = onPrevious, enabled = state.hasOlder) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.history_previous))
        }
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(
                    R.string.home_period_range,
                    state.selected.startDate.format(rangeFormat),
                    state.selected.endDate.format(rangeFormat),
                ),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                textAlign = TextAlign.Center,
            )
            Text(state.selected.endDate.format(yearFormat), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalIconButton(onClick = onNext, enabled = state.hasNewer) {
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = stringResource(R.string.history_next))
        }
    }
}

/** A day as a coloured tile: soft status colour, the pocket change in a strong pill, then that day's expenses. */
@Composable
private fun DayTile(day: HistoryDay, isToday: Boolean, onExpenseClick: (Long) -> Unit, modifier: Modifier = Modifier) {
    val formatter = LocalMoneyFormatter.current
    val a = day.allowance
    val (tile, onTile) = when (a.status) {
        AllowanceStatus.ON_TRACK -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        AllowanceStatus.LOW -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        AllowanceStatus.OVER -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    // Today breaks the shape pattern so it stands out from the finished days.
    val shape = if (isToday) {
        MaterialTheme.shapes.extraLargeIncreased.copy(bottomStart = MaterialTheme.shapes.small.bottomStart)
    } else {
        MaterialTheme.shapes.extraLarge
    }
    Surface(color = tile, contentColor = onTile, shape = shape, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).semantics(mergeDescendants = true) { heading() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (isToday) stringResource(R.string.history_today) else a.day.format(dayFormat),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                    )
                    Text(
                        stringResource(R.string.history_spent_of, formatter.format(a.spentPaise), formatter.formatWhole(a.basePaise)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    StatusLabel(a, MaterialTheme.typography.labelLarge)
                }
                // Whichever status colour differs from the tile, so the pill never blends in
                // (the chip matches the tile for "over" in light mode, the header colour does in dark).
                val pill = KishaTheme.colors.status(a.status)
                val (pillColor, onPill) = if (pill.chip == tile) pill.container to pill.content else pill.chip to pill.onChip
                day.pocketDeltaPaise?.let { PocketPill(it, pillColor, onPill) }
            }
            day.expenses.forEach { row ->
                ExpenseListItem(row, onClick = { onExpenseClick(row.expense.id) })
            }
        }
    }
}

@Composable
private fun PocketPill(deltaPaise: Long, color: Color, onColor: Color) {
    val formatter = LocalMoneyFormatter.current
    val description = if (deltaPaise >= 0) {
        stringResource(R.string.home_pocket_in, formatter.formatWhole(deltaPaise))
    } else {
        stringResource(R.string.home_pocket_out, formatter.formatWhole(-deltaPaise))
    }
    Surface(
        color = color,
        contentColor = onColor,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Savings, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                (if (deltaPaise >= 0) "+" else "") + formatter.formatWhole(deltaPaise),
                style = MaterialTheme.typography.labelLargeEmphasized,
            )
        }
    }
}
