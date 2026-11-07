package com.example.kisha.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.kisha.AppContainer
import com.example.kisha.R
import com.example.kisha.data.db.BudgetConfigEntity
import com.example.kisha.ui.format.LocalMoneyFormatter
import com.example.kisha.ui.format.MoneyFormatter
import com.example.kisha.ui.main.MainScaffold
import com.example.kisha.ui.onboarding.OnboardingScreen

private sealed interface ConfigState {
    data object Loading : ConfigState
    data class Loaded(val config: BudgetConfigEntity?) : ConfigState
}

/** App root: a loading indicator, then onboarding until a budget exists, then the main screens. */
@Composable
fun KishaAppUi(container: AppContainer) {
    val state by produceState<ConfigState>(ConfigState.Loading, container) {
        container.budgetRepository.config.collect { value = ConfigState.Loaded(it) }
    }
    when (val s = state) {
        ConfigState.Loading -> {
            val label = stringResource(R.string.loading)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator(Modifier.semantics { contentDescription = label })
            }
        }
        is ConfigState.Loaded -> {
            val config = s.config
            if (config == null) {
                OnboardingScreen(container)
            } else {
                val formatter = remember(config.currency, config.indianGrouping) {
                    MoneyFormatter(config.currency, config.indianGrouping)
                }
                CompositionLocalProvider(LocalMoneyFormatter provides formatter) {
                    MainScaffold(container)
                }
            }
        }
    }
}
