package io.github.highlandev210.kisha.allowance

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** One expense or income amount on a local date. */
data class Entry(val occurredOn: LocalDate, val amountPaise: Long)

/** A period's money snapshot: income (I) and what is reserved up front (F = fixed + savings). */
data class PeriodBudget(
    val range: DateRange,
    val incomePaise: Long,
    val fixedTotalPaise: Long,
    val savingsTargetPaise: Long,
)

enum class AllowanceStatus { ON_TRACK, LOW, OVER }

data class DayAllowance(
    /** The requested day, clamped into the period. */
    val day: LocalDate,
    /** base_d = (I − F − S<d) ÷ D_d, floored. */
    val basePaise: Long,
    /** S_d: spending on [day]. */
    val spentPaise: Long,
    /** base_d − S_d: what is left to spend on [day]. Negative when overspent. */
    val remainingPaise: Long,
    /** I + extra income up to [day] − F − all spending up to and including [day]. */
    val periodLeftPaise: Long,
    /** D_d: days left in the period including [day]; at least 1. */
    val daysLeft: Int,
    val status: AllowanceStatus,
)

/** Spread-mode allowance: base_d = (I − F − S<d) / D_d and today = base_d − S_d. */
object AllowanceEngine {
    fun dayAllowance(
        budget: PeriodBudget,
        expenses: List<Entry>,
        extraIncome: List<Entry>,
        day: LocalDate,
    ): DayAllowance {
        val range = budget.range
        val d = range.clamp(day)

        var spentBefore = 0L
        var spentToday = 0L
        for (expense in expenses) {
            val on = range.clamp(expense.occurredOn)
            when {
                on.isBefore(d) -> spentBefore += expense.amountPaise
                on == d -> spentToday += expense.amountPaise
            }
        }
        val extra = extraIncome
            .filter { !range.clamp(it.occurredOn).isAfter(d) }
            .sumOf { it.amountPaise }

        val flexible = budget.incomePaise + extra - budget.fixedTotalPaise - budget.savingsTargetPaise
        val daysLeft = ChronoUnit.DAYS.between(d, range.endInclusive).toInt() + 1
        val base = Math.floorDiv(flexible - spentBefore, daysLeft.toLong())
        val remaining = base - spentToday

        return DayAllowance(
            day = d,
            basePaise = base,
            spentPaise = spentToday,
            remainingPaise = remaining,
            periodLeftPaise = flexible - spentBefore - spentToday,
            daysLeft = daysLeft,
            status = status(base, remaining),
        )
    }

    /** OVER below zero; LOW when under 20% of the base (or no base at all); otherwise ON_TRACK. */
    fun status(basePaise: Long, remainingPaise: Long): AllowanceStatus = when {
        remainingPaise < 0 -> AllowanceStatus.OVER
        basePaise <= 0 -> AllowanceStatus.LOW
        remainingPaise * 5 < basePaise -> AllowanceStatus.LOW
        else -> AllowanceStatus.ON_TRACK
    }
}
