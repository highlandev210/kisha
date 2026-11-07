package com.example.kisha.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kisha.R
import com.example.kisha.allowance.AllowanceStatus
import com.example.kisha.allowance.DayAllowance
import com.example.kisha.allowance.Pocket
import com.example.kisha.data.db.ExpenseEntity
import com.example.kisha.ui.TestTags
import com.example.kisha.ui.common.ExpenseListItem
import com.example.kisha.ui.common.ExpenseRow
import com.example.kisha.ui.format.LocalMoneyFormatter
import com.example.kisha.ui.theme.KishaTheme
import com.example.kisha.ui.theme.StatusColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin

@Composable
fun HomeScreen(viewModel: HomeViewModel, onExpenseClick: (Long) -> Unit, snackbarHostState: SnackbarHostState) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        HomeUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        is HomeUiState.Ready -> HomeContent(s, viewModel, onExpenseClick, snackbarHostState)
    }
}

@Composable
private fun HomeContent(
    s: HomeUiState.Ready,
    viewModel: HomeViewModel,
    onExpenseClick: (Long) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val formatter = LocalMoneyFormatter.current
    val scope = rememberCoroutineScope()
    val effects = MaterialTheme.motionScheme.slowEffectsSpec<Color>()
    val status = KishaTheme.colors.status(s.allowance.status)
    val header by animateColorAsState(status.container, effects, label = "header")
    val onHeader by animateColorAsState(status.content, effects, label = "onHeader")
    val deletedMessage = stringResource(R.string.expense_deleted)
    val undoLabel = stringResource(R.string.undo)

    // Pocket hero moment: a coin flies from today's number into the pocket for each unseen finished day.
    var boxOrigin by remember { mutableStateOf(Offset.Zero) }
    var numberAt by remember { mutableStateOf(Offset.Zero) }
    var pocketAt by remember { mutableStateOf(Offset.Zero) }
    val flight = remember { Animatable(0f) }
    val bagScale = remember { Animatable(1f) }
    val shake = remember { Animatable(0f) }
    var coin by remember { mutableStateOf<PocketCelebration?>(null) }
    val celebration = s.celebration
    var revealed by remember(celebration?.throughDay) { mutableStateOf(celebration == null) }
    // Whole rupees, matching the pocket figure and the coin.
    val pocketIn = celebration?.let { stringResource(R.string.home_pocket_in, formatter.formatWhole(it.deltaPaise)) }
    val pocketOut = celebration?.let { stringResource(R.string.home_pocket_out, formatter.formatWhole(-it.deltaPaise)) }

    LaunchedEffect(celebration) {
        val c = celebration ?: return@LaunchedEffect
        delay(600)
        if (c.deltaPaise > 0) {
            coin = c
            flight.snapTo(0f)
            flight.animateTo(1f, tween(durationMillis = 950, easing = FastOutSlowInEasing))
            coin = null
            launch { bagScale.snapTo(0.78f); bagScale.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMediumLow)) }
        } else if (c.deltaPaise < 0) {
            shake.animateTo(0f, keyframes { durationMillis = 520; -14f at 80; 14f at 170; -9f at 260; 9f at 350; 0f at 520 })
        }
        revealed = true
        // Snackbar first, on a scope that outlives this effect (marking it shown clears the celebration).
        if (c.deltaPaise != 0L) scope.launch { snackbarHostState.showSnackbar(if (c.deltaPaise > 0) pocketIn!! else pocketOut!!) }
        viewModel.markCelebrated(c)
    }

    Box(Modifier.fillMaxSize().onGloballyPositioned { boxOrigin = it.positionInRoot() }) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
            // Bottom padding keeps the last row clear of the FAB.
            contentPadding = PaddingValues(bottom = 112.dp),
        ) {
            item(key = "header") {
                Header(s.allowance, header, onHeader, status, onNumberPositioned = { numberAt = it })
            }
            item(key = "sheet") {
                SheetTop(
                    s = s,
                    header = header,
                    pocketPaise = if (revealed) s.pocket.amountPaise else maxOf(0L, s.pocket.amountPaise - (celebration?.deltaPaise ?: 0L)),
                    bagScale = bagScale.value,
                    shake = shake.value,
                    onPocketPositioned = { pocketAt = it },
                )
            }
            if (s.todayExpenses.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.home_no_expenses),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp).animateItem(),
                    )
                }
            }
            items(s.todayExpenses, key = { it.expense.id }) { row ->
                SwipeToDeleteRow(
                    row = row,
                    onClick = { onExpenseClick(row.expense.id) },
                    onDelete = {
                        viewModel.delete(row.expense)
                        // One snackbar at a time, and its Undo restores exactly the expense it announced.
                        snackbarHostState.currentSnackbarData?.dismiss()
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(deletedMessage, undoLabel, duration = SnackbarDuration.Short)
                            if (result == SnackbarResult.ActionPerformed) viewModel.restore(row.expense)
                        }
                    },
                    modifier = Modifier.animateItem(
                        placementSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
                    ),
                )
            }
        }

        coin?.let { c -> FlyingCoin(c, from = numberAt - boxOrigin, to = pocketAt - boxOrigin, progress = flight.value) }
    }
}

/** UI-3, DP-1, DP-7: the hero — full-bleed status colour, the giant number, the chip. */
@Composable
private fun Header(
    allowance: DayAllowance,
    color: Color,
    onColor: Color,
    status: StatusColors,
    onNumberPositioned: (Offset) -> Unit,
) {
    // The burst turns a little every time the number changes.
    val turns = remember { Animatable(0f) }
    LaunchedEffect(allowance.remainingPaise) {
        turns.animateTo(turns.targetValue + 45f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessVeryLow))
    }
    Box(Modifier.fillMaxWidth().background(color)) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-30).dp)
                .size(240.dp)
                .graphicsLayer { rotationZ = turns.value }
                .clip(MaterialShapes.Sunny.toShape())
                .background(onColor.copy(alpha = 0.13f)),
        )
        Column(
            Modifier.statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.home_today_label), style = MaterialTheme.typography.titleMedium, color = onColor.copy(alpha = 0.85f))
            HeroNumber(
                paise = allowance.remainingPaise,
                color = onColor,
                modifier = Modifier.onGloballyPositioned { onNumberPositioned(it.positionInRoot()) },
            )
            StatusChip(allowance, status)
        }
    }
}

/** The allowance springs to its new value with a little squash, so saving an expense feels physical. */
@Composable
private fun HeroNumber(paise: Long, color: Color, modifier: Modifier = Modifier) {
    val formatter = LocalMoneyFormatter.current
    val rupees = Math.floorDiv(paise, 100L).toFloat()
    val value = remember { Animatable(rupees) }
    val squash = remember { Animatable(1f) }
    LaunchedEffect(rupees) {
        if (value.value != rupees) {
            launch { squash.snapTo(0.86f); squash.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow)) }
            value.animateTo(rupees, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
        }
    }
    val text = if (value.isRunning) formatter.formatWhole(value.value.roundToLong() * 100) else formatter.formatWhole(paise)
    Text(
        text,
        style = KishaTheme.heroNumber,
        color = color,
        maxLines = 1,
        autoSize = KishaTheme.heroAutoSize,
        modifier = modifier
            .graphicsLayer {
                scaleY = squash.value
                scaleX = 2f - squash.value
                transformOrigin = TransformOrigin(0f, 1f)
            }
            .testTag(TestTags.ALLOWANCE),
    )
}

/** DP-6: state in words and an icon, never colour alone. */
@Composable
private fun StatusChip(allowance: DayAllowance, colors: StatusColors) {
    val formatter = LocalMoneyFormatter.current
    val (icon, label) = when (allowance.status) {
        AllowanceStatus.ON_TRACK -> Icons.Filled.CheckCircle to stringResource(R.string.status_on_track)
        AllowanceStatus.LOW -> Icons.Filled.Warning to stringResource(R.string.status_low)
        AllowanceStatus.OVER -> Icons.Filled.Error to stringResource(R.string.status_over, formatter.format(-allowance.remainingPaise))
    }
    Surface(color = colors.chip, contentColor = colors.onChip, shape = MaterialTheme.shapes.extraLarge) {
        Row(
            Modifier.padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null)
            Text(label, style = MaterialTheme.typography.labelLargeEmphasized)
            if (allowance.status != AllowanceStatus.OVER && allowance.spentPaise > 0) {
                Text(stringResource(R.string.home_spent_suffix, formatter.format(allowance.spentPaise)), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** The rounded sheet that overlaps the header: pocket, period figures, then today's list. */
@Composable
private fun SheetTop(
    s: HomeUiState.Ready,
    header: Color,
    pocketPaise: Long,
    bagScale: Float,
    shake: Float,
    onPocketPositioned: (Offset) -> Unit,
) {
    val formatter = LocalMoneyFormatter.current
    val top = MaterialTheme.shapes.extraLargeIncreased.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize)
    val progressLabel = stringResource(R.string.home_period_progress)
    Box(Modifier.fillMaxWidth().background(header)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(top)
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PocketCard(s.pocket, pocketPaise, bagScale, shake, onPocketPositioned)
            Row(Modifier.padding(horizontal = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(formatter.format(s.allowance.periodLeftPaise), style = KishaTheme.figure)
                    Text(stringResource(R.string.home_left_this_period), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f)) {
                    Text("${s.allowance.daysLeft}", style = KishaTheme.figure)
                    Text(
                        pluralStringResource(R.plurals.home_days_to_go, s.allowance.daysLeft),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            LinearWavyProgressIndicator(
                progress = { s.elapsedFraction },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).semantics { contentDescription = progressLabel },
            )
            Text(
                stringResource(R.string.home_spent_today),
                style = MaterialTheme.typography.titleMediumEmphasized,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
private fun PocketCard(pocket: Pocket, shownPaise: Long, bagScale: Float, shake: Float, onPositioned: (Offset) -> Unit) {
    val formatter = LocalMoneyFormatter.current
    val density = LocalDensity.current
    val rupees by animateFloatAsState(Math.floorDiv(shownPaise, 100L).toFloat(), tween(900), label = "pocket")
    val shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = MaterialTheme.shapes.small.bottomStart)
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = shape,
        modifier = Modifier.fillMaxWidth().graphicsLayer { translationX = with(density) { shake.dp.toPx() } },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier
                    .size(56.dp)
                    .onGloballyPositioned { onPositioned(it.positionInRoot()) }
                    .graphicsLayer { scaleX = bagScale; scaleY = bagScale }
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Column {
                Text(stringResource(R.string.home_pocket_label), style = MaterialTheme.typography.labelLarge)
                Text(formatter.formatWhole(rupees.roundToLong() * 100), style = KishaTheme.figure)
                Text(
                    when {
                        pocket.streakDays > 0 -> pluralStringResource(R.plurals.home_pocket_streak, pocket.streakDays, pocket.streakDays)
                        pocket.amountPaise > 0 -> stringResource(R.string.home_pocket_keep)
                        else -> stringResource(R.string.home_pocket_start)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** The coin, arcing from today's number into the pocket. */
@Composable
private fun FlyingCoin(celebration: PocketCelebration, from: Offset, to: Offset, progress: Float) {
    val formatter = LocalMoneyFormatter.current
    val arc = with(LocalDensity.current) { 140.dp.toPx() }
    val x = lerp(from.x, to.x, progress)
    val y = lerp(from.y, to.y, progress) - arc * sin(PI * progress).toFloat()
    val scale = if (progress < 0.3f) lerp(0.4f, 1.15f, progress / 0.3f) else lerp(1.15f, 0.7f, (progress - 0.3f) / 0.7f)
    Box(
        Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(64.dp)
            .graphicsLayer { rotationZ = 220f * progress; scaleX = scale; scaleY = scale }
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(KishaTheme.colors.coin),
        contentAlignment = Alignment.Center,
    ) {
        Text("+" + formatter.formatWhole(celebration.deltaPaise), style = MaterialTheme.typography.labelMediumEmphasized, color = KishaTheme.colors.onCoin, maxLines = 1)
    }
}

@Composable
private fun SwipeToDeleteRow(row: ExpenseRow, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val deleteLabel = stringResource(R.string.delete)
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = { if (it == SwipeToDismissBoxValue.EndToStart) onDelete() },
        backgroundContent = {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 24.dp), contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
        modifier = modifier.semantics {
            customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
        },
    ) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ExpenseListItem(row, onClick = onClick, modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}
