package io.github.highlandev210.kisha.allowance

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PocketEngineTest {
    private fun d(day: Int) = LocalDate.of(2026, 11, day)
    private fun e(day: Int, paise: Long) = Entry(d(day), paise)

    // Spec worked example: ₹24,000 flexible over November (30 days) → ₹800 on day 1.
    private val nov = PeriodBudget(DateRange(d(1), LocalDate.of(2026, 11, 30)), 6_000_000, 3_600_000, 0)

    private fun pocket(expenses: List<Entry>, today: LocalDate, countFrom: LocalDate = d(1), income: List<Entry> = emptyList()) =
        PocketEngine.pocket(nov, expenses, income, today, countFrom)

    @Test fun emptyOnTheFirstDay() {
        val p = pocket(emptyList(), today = d(1))
        assertEquals(0L, p.amountPaise)
        assertEquals(0, p.streakDays)
        assertEquals(emptyList<PocketDay>(), p.days)
    }

    @Test fun underspendDropsIntoThePocket() {
        val p = pocket(listOf(e(1, 50_000)), today = d(2))
        assertEquals(30_000L, p.amountPaise) // ₹800 − ₹500
        assertEquals(1, p.streakDays)
        assertEquals(listOf(PocketDay(d(1), 30_000)), p.days)
    }

    @Test fun overspendTakesFromThePocketButNeverBelowZero() {
        // Day 1: nothing spent → +₹800. Day 2: base ₹827.58 → floor 82_758; spend ₹2,000 → −117_242.
        val p = pocket(listOf(e(2, 200_000)), today = d(3))
        assertEquals(listOf(PocketDay(d(1), 80_000), PocketDay(d(2), -117_242)), p.days)
        assertEquals(0L, p.amountPaise)
        assertEquals(0, p.streakDays)
    }

    @Test fun streakCountsRecentDaysUnderBudget() {
        // Day 1 over, days 2–3 under → streak 2.
        val p = pocket(listOf(e(1, 100_000), e(2, 10_000), e(3, 10_000)), today = d(4))
        assertEquals(2, p.streakDays)
    }

    @Test fun daysBeforeSetupDoNotCount() {
        // Set up on the 7th: the 1st–6th must not fill the pocket.
        val p = pocket(emptyList(), today = d(8), countFrom = d(7))
        assertEquals(listOf(d(7)), p.days.map { it.day })
        assertEquals(100_000L, p.amountPaise) // ₹24,000 ÷ 24 days left on the 7th
    }

    @Test fun closedPeriodCountsThroughItsLastDay() {
        val p = pocket(emptyList(), today = LocalDate.of(2026, 12, 3))
        assertEquals(30, p.days.size)
        assertEquals(LocalDate.of(2026, 11, 30), p.days.last().day)
    }

    @Test fun todayIsNeverCountedUntilItEnds() {
        val p = pocket(listOf(e(5, 10_000)), today = d(5))
        assertEquals(d(4), p.days.last().day)
    }
}
