package io.github.highlandev210.kisha.ui.format

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MoneyFormatterTest {
    private val f = MoneyFormatter()

    @Test fun indianGroupingWithPaiseOnlyWhenNonZero() {
        assertEquals("₹1,00,000", f.format(10_000_000))
        assertEquals("₹12,34,567.89", f.format(123_456_789))
        assertEquals("₹12.50", f.format(1_250))
        assertEquals("₹0", f.format(0))
    }

    @Test fun negativeAmountsKeepSignBeforeSymbol() {
        assertEquals("-₹500", f.format(-50_000))
    }

    @Test fun wholeRupeesFloor() {
        assertEquals("₹810", f.formatWhole(81_034))
        assertEquals("-₹811", f.formatWhole(-81_034))
    }

    @Test fun internationalGroupingAndCustomSymbol() {
        assertEquals("$100,000", MoneyFormatter("$", indianGrouping = false).format(10_000_000))
    }
}
