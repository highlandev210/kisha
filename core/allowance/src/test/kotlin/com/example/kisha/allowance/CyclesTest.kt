package com.example.kisha.allowance

import com.example.kisha.allowance.CycleStart.DayOfMonth
import com.example.kisha.allowance.CycleStart.LastWorkingDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

class CyclesTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private val m1 = DayOfMonth(1)

    @Test fun calendarMonthLengths() {
        assertEquals(DateRange(d(2026, 11, 1), d(2026, 11, 30)), Cycles.periodContaining(d(2026, 11, 7), m1))
        assertEquals(31, Cycles.periodContaining(d(2026, 12, 25), m1).lengthInDays)
    }

    @Test fun februaryNonLeapAndLeap() {
        assertEquals(DateRange(d(2027, 2, 1), d(2027, 2, 28)), Cycles.periodContaining(d(2027, 2, 10), m1))
        assertEquals(DateRange(d(2028, 2, 1), d(2028, 2, 29)), Cycles.periodContaining(d(2028, 2, 10), m1))
    }

    @Test fun paydayCycleSpansMonths() {
        assertEquals(DateRange(d(2026, 11, 7), d(2026, 12, 6)), Cycles.periodContaining(d(2026, 11, 7), DayOfMonth(7)))
        assertEquals(DateRange(d(2026, 10, 7), d(2026, 11, 6)), Cycles.periodContaining(d(2026, 11, 6), DayOfMonth(7)))
    }

    @Test fun paydayOn31stClampsToShortMonths() {
        assertEquals(DateRange(d(2027, 1, 31), d(2027, 2, 27)), Cycles.periodContaining(d(2027, 2, 15), DayOfMonth(31)))
        assertEquals(DateRange(d(2027, 2, 28), d(2027, 3, 30)), Cycles.periodContaining(d(2027, 3, 1), DayOfMonth(31)))
        assertEquals(DateRange(d(2028, 2, 29), d(2028, 3, 30)), Cycles.periodContaining(d(2028, 2, 29), DayOfMonth(31)))
    }

    @Test fun lastWorkingDaySkipsWeekend() {
        assertEquals(d(2026, 10, 30), Cycles.startIn(YearMonth.of(2026, 10), LastWorkingDay)) // Oct 31 is a Saturday
        assertEquals(d(2027, 1, 29), Cycles.startIn(YearMonth.of(2027, 1), LastWorkingDay)) // Jan 31 is a Sunday
        assertEquals(DateRange(d(2026, 10, 30), d(2026, 11, 29)), Cycles.periodContaining(d(2026, 11, 7), LastWorkingDay))
    }

    @Test fun periodsAfterFillsGaps() {
        assertEquals(
            listOf(
                DateRange(d(2026, 12, 1), d(2026, 12, 31)),
                DateRange(d(2027, 1, 1), d(2027, 1, 31)),
                DateRange(d(2027, 2, 1), d(2027, 2, 28)),
            ),
            Cycles.periodsAfter(d(2026, 11, 30), d(2027, 2, 3), m1),
        )
    }

    @Test fun periodsAfterEmptyWhenTodayNotPastEnd() {
        assertEquals(emptyList<DateRange>(), Cycles.periodsAfter(d(2026, 11, 30), d(2026, 11, 30), m1))
        assertEquals(emptyList<DateRange>(), Cycles.periodsAfter(d(2026, 11, 30), d(2026, 11, 15), m1))
    }

    @Test fun storedEncodingRoundTrips() {
        assertEquals(DayOfMonth(7), CycleStart.fromStored(7))
        assertEquals(LastWorkingDay, CycleStart.fromStored(-1))
        assertEquals(31, DayOfMonth(31).stored)
        assertThrows(IllegalArgumentException::class.java) { CycleStart.fromStored(0) }
    }

    @Test fun instantToLocalDateUsesZone() {
        val t = Instant.parse("2026-11-07T18:45:00Z")
        assertEquals(d(2026, 11, 8), t.localDateIn(ZoneId.of("Asia/Kolkata")))
        assertEquals(d(2026, 11, 7), t.localDateIn(ZoneOffset.UTC))
    }
}
