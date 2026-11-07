package io.github.highlandev210.kisha.allowance

import java.time.LocalDate

/** What one finished day put into (+) or took out of (−) the pocket: that day's allowance left over. */
data class PocketDay(val day: LocalDate, val deltaPaise: Long)

data class Pocket(
    /** Never negative: overspending empties the pocket but doesn't dig below zero. */
    val amountPaise: Long,
    /** Consecutive most recent finished days that stayed within their allowance. */
    val streakDays: Int,
    /** Every finished day counted, oldest first. */
    val days: List<PocketDay>,
) {
    /** What the pocket held at the end of [day] (0 when null or before the first day). */
    fun amountThrough(day: LocalDate?): Long = if (day == null) {
        0L
    } else {
        days.filter { !it.day.isAfter(day) }.fold(0L) { total, d -> maxOf(0L, total + d.deltaPaise) }
    }
}

/**
 * The pocket collects what you didn't spend of each finished day's allowance. Only days that have
 * ended (before [today]) and fall on or after [countFrom] (the setup day) count.
 */
object PocketEngine {
    fun pocket(
        budget: PeriodBudget,
        expenses: List<Entry>,
        extraIncome: List<Entry>,
        today: LocalDate,
        countFrom: LocalDate,
    ): Pocket {
        val range = budget.range
        val first = maxOf(range.start, countFrom)
        val last = minOf(today.minusDays(1), range.endInclusive)
        val days = generateSequence(first) { it.plusDays(1) }
            .takeWhile { !it.isAfter(last) }
            .map { day -> PocketDay(day, AllowanceEngine.dayAllowance(budget, expenses, extraIncome, day).remainingPaise) }
            .toList()
        val amount = days.fold(0L) { total, d -> maxOf(0L, total + d.deltaPaise) }
        val streak = days.asReversed().takeWhile { it.deltaPaise >= 0 }.size
        return Pocket(amount, streak, days)
    }
}
