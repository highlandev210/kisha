package com.example.kisha.allowance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test fun parsesPlainDecimalAndGrouped() {
        listOf(
            "250" to 25_000L, "250.5" to 25_050L, "250.50" to 25_050L, "0.75" to 75L, ".5" to 50L,
            "1,00,000" to 10_000_000L, "100,000" to 10_000_000L, " 42 " to 4_200L, "0" to 0L,
            "999999999.99" to 99_999_999_999L,
        ).forEach { (text, paise) -> assertEquals(text, paise, Money.parsePaise(text)) }
    }

    @Test fun rejectsInvalid() {
        listOf("", ".", "abc", "1.234", "-5", "1..2", "1e5", "₹50", "1000000000")
            .forEach { assertNull(it, Money.parsePaise(it)) }
    }
}
