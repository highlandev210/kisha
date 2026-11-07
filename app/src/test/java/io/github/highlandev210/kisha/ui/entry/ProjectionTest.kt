package io.github.highlandev210.kisha.ui.entry

import io.github.highlandev210.kisha.allowance.AllowanceStatus
import io.github.highlandev210.kisha.allowance.DayAllowance
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ProjectionTest {
    // ₹800 today, ₹500 already spent → ₹300 left.
    private val today = DayAllowance(LocalDate.of(2026, 11, 1), 80_000, 50_000, 30_000, 2_350_000, 30, AllowanceStatus.ON_TRACK)

    @Test fun nothingTypedKeepsTodaysState() =
        assertEquals(Projection(30_000, AllowanceStatus.ON_TRACK), project(today, 0))

    @Test fun typingTowardsTheLimitTurnsLow() =
        assertEquals(Projection(5_000, AllowanceStatus.LOW), project(today, 25_000))

    @Test fun typingPastTheLimitTurnsOver() =
        assertEquals(Projection(-10_000, AllowanceStatus.OVER), project(today, 40_000))
}
