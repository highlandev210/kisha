package com.example.kisha.data

import com.example.kisha.allowance.DateRange
import com.example.kisha.allowance.Entry
import com.example.kisha.allowance.PeriodBudget
import com.example.kisha.allowance.localDateIn
import com.example.kisha.data.db.ExpenseEntity
import com.example.kisha.data.db.IncomeEntity
import com.example.kisha.data.db.PeriodEntity
import java.time.ZoneId

fun PeriodEntity.range(): DateRange = DateRange(startDate, endDate)

fun PeriodEntity.toBudget(): PeriodBudget = PeriodBudget(
    range = range(),
    incomePaise = income,
    fixedTotalPaise = fixedTotal,
    savingsTargetPaise = savingsTarget,
)

fun ExpenseEntity.toEntry(zone: ZoneId): Entry = Entry(occurredAt.localDateIn(zone), amountPaise)

fun IncomeEntity.toEntry(zone: ZoneId): Entry = Entry(occurredAt.localDateIn(zone), amountPaise)
