package io.github.highlandev210.kisha

import android.content.Context
import io.github.highlandev210.kisha.data.BudgetRepository
import io.github.highlandev210.kisha.data.ExpenseRepository
import io.github.highlandev210.kisha.data.SystemTimeSource
import io.github.highlandev210.kisha.data.TimeSource
import io.github.highlandev210.kisha.data.db.KishaDatabase
import io.github.highlandev210.kisha.data.todayFlow
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Manual DI: every long-lived object the app needs, created once in [KishaApp]. */
class AppContainer(
    val database: KishaDatabase,
    val time: TimeSource,
    val today: Flow<LocalDate>,
) {
    val budgetRepository = BudgetRepository(database, time)
    val expenseRepository = ExpenseRepository(database, time)

    companion object {
        fun create(context: Context): AppContainer {
            val appContext = context.applicationContext
            return AppContainer(
                database = KishaDatabase.build(appContext),
                time = SystemTimeSource,
                today = todayFlow(appContext, SystemTimeSource),
            )
        }
    }
}
