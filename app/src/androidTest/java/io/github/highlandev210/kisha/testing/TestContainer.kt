package io.github.highlandev210.kisha.testing

import androidx.test.core.app.ApplicationProvider
import io.github.highlandev210.kisha.AppContainer
import io.github.highlandev210.kisha.data.db.KishaDatabase
import io.github.highlandev210.kisha.data.today
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

/** An AppContainer over an in-memory database, a fixed clock and a "today" the test controls. */
fun testContainer(
    time: FixedTimeSource,
    today: MutableStateFlow<LocalDate> = MutableStateFlow(time.today()),
): AppContainer = AppContainer(
    database = KishaDatabase.inMemory(ApplicationProvider.getApplicationContext()),
    time = time,
    today = today,
)
