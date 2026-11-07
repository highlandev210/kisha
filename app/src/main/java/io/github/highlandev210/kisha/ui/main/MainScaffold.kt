package io.github.highlandev210.kisha.ui.main

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import io.github.highlandev210.kisha.AppContainer
import io.github.highlandev210.kisha.R
import io.github.highlandev210.kisha.data.db.ExpenseEntity
import io.github.highlandev210.kisha.ui.entry.EntryTarget
import io.github.highlandev210.kisha.ui.entry.EntryViewModel
import io.github.highlandev210.kisha.ui.entry.QuickEntrySheet
import io.github.highlandev210.kisha.ui.history.HistoryScreen
import io.github.highlandev210.kisha.ui.history.HistoryViewModel
import io.github.highlandev210.kisha.ui.home.HomeScreen
import io.github.highlandev210.kisha.ui.home.HomeUiState
import io.github.highlandev210.kisha.ui.home.HomeViewModel
import io.github.highlandev210.kisha.ui.settings.SettingsScreen
import io.github.highlandev210.kisha.ui.settings.SettingsViewModel
import io.github.highlandev210.kisha.ui.theme.KishaTheme

enum class Tab(val label: Int, val icon: ImageVector) {
    TODAY(R.string.tab_today, Icons.Outlined.Today),
    HISTORY(R.string.tab_history, Icons.Outlined.History),
    SETTINGS(R.string.tab_settings, Icons.Outlined.Settings),
}

/** UI-6: short navigation bar on phones, navigation rail on wide screens; labels always shown (DP-5). */
@Composable
fun MainScaffold(container: AppContainer) {
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    val snackbarHostState = remember { SnackbarHostState() }
    // Undo lives here, not in a tab's screen, so switching tabs doesn't cancel it.
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.expense_deleted)
    val undoLabel = stringResource(R.string.undo)
    val offerUndo: (ExpenseEntity, (ExpenseEntity) -> Unit) -> Unit = { expense, restore ->
        // One snackbar at a time, and its Undo restores exactly the expense it announced.
        snackbarHostState.currentSnackbarData?.dismiss()
        scope.launch {
            val result = snackbarHostState.showSnackbar(deletedMessage, undoLabel, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) restore(expense)
        }
    }
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val homeViewModel = viewModel {
        HomeViewModel(container.budgetRepository, container.expenseRepository, container.today, container.time)
    }
    val historyViewModel = viewModel {
        HistoryViewModel(container.budgetRepository, container.expenseRepository, container.today, container.time)
    }
    val settingsViewModel = viewModel { SettingsViewModel(container.budgetRepository, container.today) }
    val entryViewModel = viewModel {
        EntryViewModel(container.expenseRepository, container.budgetRepository, container.time)
    }
    val openExpense: (Long) -> Unit = { entryViewModel.open(EntryTarget.EditExpense(it)) }

    // Home's header is a deep colour in both themes, so its status-bar icons are always light.
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()
    DisposableEffect(tab, darkTheme) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = !darkTheme && tab != Tab.TODAY
        onDispose { if (controller != null && previous != null) controller.isAppearanceLightStatusBars = previous }
    }

    Row(Modifier.fillMaxSize()) {
        if (wide) {
            NavigationRail {
                Tab.entries.forEach { t ->
                    NavigationRailItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(stringResource(t.label)) },
                        alwaysShowLabel = true,
                    )
                }
            }
        }
        Scaffold(
            modifier = Modifier.weight(1f),
            // Screens handle the status bar themselves so Home's header can run edge to edge.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!wide) {
                    ShortNavigationBar {
                        Tab.entries.forEach { t ->
                            ShortNavigationBarItem(
                                selected = tab == t,
                                onClick = { tab = t },
                                icon = { Icon(t.icon, contentDescription = null) },
                                label = { Text(stringResource(t.label)) },
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (tab == Tab.TODAY) {
                    // Decision 1: one tap opens expense entry (HM-5); income is a toggle inside the sheet.
                    // The FAB clears its label's semantics, so name it explicitly for TalkBack (NF-6).
                    val addLabel = stringResource(R.string.add)
                    MediumExtendedFloatingActionButton(
                        text = { Text(addLabel) },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        onClick = { entryViewModel.open(EntryTarget.NewExpense) },
                        modifier = Modifier.semantics { contentDescription = addLabel },
                        // Warm peach against the teal header (DP-1: strong container colour).
                        containerColor = KishaTheme.colors.fab,
                        contentColor = KishaTheme.colors.onFab,
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (tab) {
                    Tab.TODAY -> HomeScreen(
                        homeViewModel,
                        onExpenseClick = openExpense,
                        onDeleted = { offerUndo(it, homeViewModel::restore) },
                        snackbarHostState = snackbarHostState,
                    )
                    Tab.HISTORY -> HistoryScreen(historyViewModel, onExpenseClick = openExpense)
                    Tab.SETTINGS -> SettingsScreen(settingsViewModel)
                }
            }
        }
    }
    val homeState by homeViewModel.state.collectAsStateWithLifecycle()
    QuickEntrySheet(
        entryViewModel,
        today = (homeState as? HomeUiState.Ready)?.allowance,
        onDeleted = { offerUndo(it, entryViewModel::restore) },
    )
}
