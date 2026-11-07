package com.example.kisha.testing

import app.cash.turbine.ReceiveTurbine
import com.example.kisha.AppContainer
import com.example.kisha.allowance.CycleStart
import com.example.kisha.data.DefaultCategory
import com.example.kisha.data.NewFixedCost
import com.example.kisha.data.SetupInput
import com.example.kisha.data.db.PeriodType
import java.time.LocalDate

val TEST_CATEGORIES = listOf(
    DefaultCategory("Food", "restaurant"),
    DefaultCategory("Transport", "directions_bus"),
    DefaultCategory("Other", "category"),
)

/** The spec's worked example: ₹60,000 income, ₹36,000 rent, calendar months. */
suspend fun AppContainer.setUpWorkedExample(today: LocalDate = LocalDate.of(2026, 11, 1)) {
    budgetRepository.completeSetup(
        SetupInput(PeriodType.MONTH, CycleStart.DayOfMonth(1), 6_000_000, listOf(NewFixedCost("Rent", 3_600_000, null))),
        TEST_CATEGORIES,
        today,
    )
}

/** Skips intermediate emissions (Room re-emits on unrelated invalidations) until [predicate] holds. */
suspend fun <T> ReceiveTurbine<T>.awaitMatching(predicate: (T) -> Boolean): T {
    while (true) {
        val item = awaitItem()
        if (predicate(item)) return item
    }
}
