package com.example.kisha.testing

import androidx.test.core.app.ApplicationProvider
import com.example.kisha.AppContainer
import com.example.kisha.data.db.KishaDatabase
import com.example.kisha.data.today
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
