package com.example.kisha

import android.content.Context
import com.example.kisha.data.BudgetRepository
import com.example.kisha.data.ExpenseRepository
import com.example.kisha.data.SystemTimeSource
import com.example.kisha.data.TimeSource
import com.example.kisha.data.db.KishaDatabase
import com.example.kisha.data.todayFlow
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
