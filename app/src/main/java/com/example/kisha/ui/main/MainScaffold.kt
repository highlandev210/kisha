package com.example.kisha.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kisha.AppContainer
import com.example.kisha.R
import com.example.kisha.ui.entry.EntryTarget
import com.example.kisha.ui.entry.EntryViewModel
import com.example.kisha.ui.entry.QuickEntrySheet
import com.example.kisha.ui.history.HistoryScreen
import com.example.kisha.ui.history.HistoryViewModel
import com.example.kisha.ui.home.HomeScreen
import com.example.kisha.ui.home.HomeViewModel

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
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val homeViewModel = viewModel {
        HomeViewModel(container.budgetRepository, container.expenseRepository, container.today, container.time)
    }
    val historyViewModel = viewModel {
        HistoryViewModel(container.budgetRepository, container.expenseRepository, container.today, container.time)
    }
    val entryViewModel = viewModel {
        EntryViewModel(container.expenseRepository, container.budgetRepository, container.time)
    }
    val openExpense: (Long) -> Unit = { entryViewModel.open(EntryTarget.EditExpense(it)) }

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
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (tab) {
                    Tab.TODAY -> HomeScreen(homeViewModel, onExpenseClick = openExpense, snackbarHostState = snackbarHostState)
                    Tab.HISTORY -> HistoryScreen(historyViewModel, onExpenseClick = openExpense)
                    Tab.SETTINGS -> Text(stringResource(R.string.tab_settings))
                }
            }
        }
    }
    QuickEntrySheet(entryViewModel)
}
