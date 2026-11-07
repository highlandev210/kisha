package com.example.kisha.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.kisha.AppContainer
import com.example.kisha.R

/** Placeholder until Task 9 adds navigation and Home. */
@Composable
fun MainScaffold(container: AppContainer) {
    Scaffold { padding ->
        Text(stringResource(R.string.tab_today), modifier = Modifier.padding(padding))
    }
}
