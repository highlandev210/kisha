package io.github.highlandev210.kisha.allowance

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** An inclusive range of local dates, e.g. one budget period. */
data class DateRange(val start: LocalDate, val endInclusive: LocalDate) {
    init {
        require(!endInclusive.isBefore(start)) { "end $endInclusive is before start $start" }
    }

    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(endInclusive)

    val lengthInDays: Int get() = ChronoUnit.DAYS.between(start, endInclusive).toInt() + 1

    fun clamp(date: LocalDate): LocalDate = when {
        date.isBefore(start) -> start
        date.isAfter(endInclusive) -> endInclusive
        else -> date
    }
}

/** The day each budget cycle starts. */
sealed interface CycleStart {
    /** How the rule is persisted: 1..31 for a day of month, -1 for the last working day. */
    val stored: Int

    /** A fixed day of month, clamped to the month's length (31 → 28 in February). */
    data class DayOfMonth(val day: Int) : CycleStart {
        init {
            require(day in 1..31) { "day must be 1..31, was $day" }
        }

        override val stored: Int get() = day
    }

    /** The last Monday–Friday of the month. Public holidays are not considered. */
    data object LastWorkingDay : CycleStart {
        override val stored: Int get() = -1
    }

    companion object {
        fun fromStored(value: Int): CycleStart = when (value) {
            -1 -> LastWorkingDay
            in 1..31 -> DayOfMonth(value)
            else -> throw IllegalArgumentException("not a stored cycle start: $value")
        }
    }
}

object Cycles {
    fun startIn(month: YearMonth, rule: CycleStart): LocalDate = when (rule) {
        is CycleStart.DayOfMonth -> month.atDay(minOf(rule.day, month.lengthOfMonth()))
        CycleStart.LastWorkingDay -> {
            var day = month.atEndOfMonth()
            while (day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY) {
                day = day.minusDays(1)
            }
            day
        }
    }

    fun periodContaining(date: LocalDate, rule: CycleStart): DateRange {
        val month = YearMonth.from(date)
        val startThisMonth = startIn(month, rule)
        return if (!date.isBefore(startThisMonth)) {
            DateRange(startThisMonth, startIn(month.plusMonths(1), rule).minusDays(1))
        } else {
            DateRange(startIn(month.minusMonths(1), rule), startThisMonth.minusDays(1))
        }
    }

    /**
     * Ranges starting at [previousEnd]+1, each ending where [periodContaining] its start ends,
     * up to and including the one containing [today]. Empty if [today] <= [previousEnd].
     */
    fun periodsAfter(previousEnd: LocalDate, today: LocalDate, rule: CycleStart): List<DateRange> {
        val ranges = mutableListOf<DateRange>()
        var start = previousEnd.plusDays(1)
        while (!today.isBefore(start)) {
            val range = DateRange(start, periodContaining(start, rule).endInclusive)
            ranges += range
            start = range.endInclusive.plusDays(1)
        }
        return ranges
    }
}

/** The calendar date of this instant on a wall clock in [zone]. */
fun Instant.localDateIn(zone: ZoneId): LocalDate = atZone(zone).toLocalDate()
