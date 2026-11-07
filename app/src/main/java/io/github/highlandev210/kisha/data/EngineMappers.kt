package io.github.highlandev210.kisha.data

import io.github.highlandev210.kisha.allowance.DateRange
import io.github.highlandev210.kisha.allowance.Entry
import io.github.highlandev210.kisha.allowance.PeriodBudget
import io.github.highlandev210.kisha.allowance.localDateIn
import io.github.highlandev210.kisha.data.db.ExpenseEntity
import io.github.highlandev210.kisha.data.db.IncomeEntity
import io.github.highlandev210.kisha.data.db.PeriodEntity
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
