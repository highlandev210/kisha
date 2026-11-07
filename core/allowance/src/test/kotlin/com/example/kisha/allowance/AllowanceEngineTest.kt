package com.example.kisha.allowance

import com.example.kisha.allowance.AllowanceStatus.LOW
import com.example.kisha.allowance.AllowanceStatus.ON_TRACK
import com.example.kisha.allowance.AllowanceStatus.OVER
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class AllowanceEngineTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private fun e(day: Int, paise: Long) = Entry(d(2026, 11, day), paise)
    private fun a(budget: PeriodBudget, expenses: List<Entry>, income: List<Entry>, day: LocalDate) =
        AllowanceEngine.dayAllowance(budget, expenses, income, day)

    // Spec worked example: ₹60,000 income, ₹36,000 fixed, November (30 days).
    private val nov = PeriodBudget(DateRange(d(2026, 11, 1), d(2026, 11, 30)), 6_000_000, 3_600_000, 0)

    @Test fun workedExampleDayOne() {
        val r = a(nov, emptyList(), emptyList(), d(2026, 11, 1))
        assertEquals(80_000, r.basePaise)
        assertEquals(80_000, r.remainingPaise)
        assertEquals(30, r.daysLeft)
        assertEquals(2_400_000, r.periodLeftPaise)
        assertEquals(ON_TRACK, r.status)
    }

    @Test fun workedExampleDayOneAfterSpending() {
        val r = a(nov, listOf(e(1, 50_000)), emptyList(), d(2026, 11, 1))
        assertEquals(50_000, r.spentPaise)
        assertEquals(30_000, r.remainingPaise)
        assertEquals(2_350_000, r.periodLeftPaise)
    }

    @Test fun workedExampleDayTwo() {
        val r = a(nov, listOf(e(1, 50_000)), emptyList(), d(2026, 11, 2))
        assertEquals(81_034, r.basePaise)
        assertEquals(29, r.daysLeft)
    }

    @Test fun overspendLowersRestOfPeriod() {
        assertEquals(75_862, a(nov, listOf(e(1, 200_000)), emptyList(), d(2026, 11, 2)).basePaise)
    }

    @Test fun extraIncomeSpreadsFromItsDay() {
        val inc = listOf(e(11, 200_000))
        assertEquals(130_000, a(nov, emptyList(), inc, d(2026, 11, 11)).basePaise)
        assertEquals(114_285, a(nov, emptyList(), inc, d(2026, 11, 10)).basePaise)
    }

    @Test fun savingsTargetReservedLikeFixedCosts() {
        val budget = nov.copy(fixedTotalPaise = 3_200_000, savingsTargetPaise = 400_000)
        assertEquals(80_000, a(budget, emptyList(), emptyList(), d(2026, 11, 1)).basePaise)
    }

    @Test fun exhaustedPeriodShowsNegative() {
        val ex = listOf(e(1, 2_500_000))
        a(nov, ex, emptyList(), d(2026, 11, 1)).let {
            assertEquals(-2_420_000, it.remainingPaise)
            assertEquals(-100_000, it.periodLeftPaise)
            assertEquals(OVER, it.status)
        }
        a(nov, ex, emptyList(), d(2026, 11, 2)).let {
            assertEquals(-3_449, it.basePaise)
            assertEquals(OVER, it.status)
        }
    }

    @Test fun incomeBelowFixedShowsNegative() {
        a(nov.copy(incomePaise = 3_000_000), emptyList(), emptyList(), d(2026, 11, 1)).let {
            assertEquals(-20_000, it.remainingPaise)
            assertEquals(OVER, it.status)
        }
    }

    @Test fun lastDayDividesByOne() {
        a(nov, listOf(e(5, 2_300_000)), emptyList(), d(2026, 11, 30)).let {
            assertEquals(100_000, it.basePaise)
            assertEquals(1, it.daysLeft)
        }
    }

    @Test fun leapFebruaryUses29Days() {
        val feb = PeriodBudget(DateRange(d(2028, 2, 1), d(2028, 2, 29)), 2_900_000, 0, 0)
        a(feb, emptyList(), emptyList(), d(2028, 2, 1)).let {
            assertEquals(100_000, it.basePaise)
            assertEquals(29, it.daysLeft)
        }
    }

    @Test fun entriesOutsideRangeAreClamped() {
        assertEquals(10_000, a(nov, listOf(Entry(d(2026, 10, 31), 10_000)), emptyList(), d(2026, 11, 1)).spentPaise)
        assertEquals(10_000, a(nov, listOf(Entry(d(2026, 12, 1), 10_000)), emptyList(), d(2026, 11, 30)).spentPaise)
    }

    @Test fun dayOutsideRangeClampsToEdge() {
        a(nov, emptyList(), emptyList(), d(2026, 10, 25)).let {
            assertEquals(d(2026, 11, 1), it.day)
            assertEquals(30, it.daysLeft)
        }
    }

    @Test fun timeZoneChangeMovesDayButKeepsPeriodTotal() {
        val t = Instant.parse("2026-11-07T18:45:00Z")
        val ist = a(nov, listOf(Entry(t.localDateIn(ZoneId.of("Asia/Kolkata")), 50_000)), emptyList(), d(2026, 11, 30))
        val utc = a(nov, listOf(Entry(t.localDateIn(ZoneOffset.UTC), 50_000)), emptyList(), d(2026, 11, 30))
        assertEquals(ist.periodLeftPaise, utc.periodLeftPaise)
        assertEquals(2_350_000, utc.periodLeftPaise)
    }

    @Test fun statusThresholds() {
        with(AllowanceEngine) {
            assertEquals(ON_TRACK, status(80_000, 16_000))
            assertEquals(LOW, status(80_000, 15_999))
            assertEquals(LOW, status(80_000, 0))
            assertEquals(OVER, status(80_000, -1))
            assertEquals(LOW, status(0, 0))
            assertEquals(OVER, status(-5, -5))
        }
    }
}
