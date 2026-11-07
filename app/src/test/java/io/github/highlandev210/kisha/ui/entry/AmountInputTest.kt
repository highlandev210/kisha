package io.github.highlandev210.kisha.ui.entry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AmountInputTest {
    private fun type(vararg keys: Key) = keys.fold(AmountInput()) { input, key -> input.press(key) }
    private fun digits(s: String) = s.map { if (it == '.') Key.Dot else Key.Digit(it.digitToInt()) }.toTypedArray()

    @Test fun typesWholeRupees() {
        val input = type(*digits("120"))
        assertEquals("120", input.text)
        assertEquals(12_000L, input.paise)
    }

    @Test fun limitsToTwoDecimals() = assertEquals("1.23", type(*digits("1.234")).text)

    @Test fun singleDot() = assertEquals("1.", type(*digits("1..")).text)

    @Test fun leadingZeroReplaced() = assertEquals("5", type(*digits("05")).text)

    @Test fun dotOnEmptyBecomesZeroDot() = assertEquals("0.", type(Key.Dot).text)

    @Test fun backspaceRemovesLastAndNoOpOnEmpty() {
        assertEquals("1", type(*digits("12"), Key.Backspace).text)
        assertEquals("", type(Key.Backspace).text)
    }

    @Test fun ignoresKeysBeyondMax() = assertEquals("999999999", type(*digits("999999999999")).text)

    @Test fun zeroCannotSave() = assertFalse(AmountInput("0").canSave)
}
