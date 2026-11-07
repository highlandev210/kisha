package io.github.highlandev210.kisha.ui.format

import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import java.math.BigDecimal
import java.util.Locale
import kotlin.math.absoluteValue

/** Formats paise for display with ICU grouping: lakh style (1,00,000) or international (100,000). */
class MoneyFormatter(val symbol: String = "₹", val indianGrouping: Boolean = true) {
    private val symbols = DecimalFormatSymbols(Locale.ENGLISH)
    private val grouping = if (indianGrouping) "#,##,##0" else "#,##0"
    private val whole = DecimalFormat(grouping, symbols)
    private val withPaise = DecimalFormat("$grouping.00", symbols)

    /** Shows paise only when they are non-zero: "₹1,00,000", "₹12.50", "-₹500". */
    @Synchronized
    fun format(paise: Long): String {
        val abs = paise.absoluteValue
        val digits = if (abs % 100 == 0L) whole.format(abs / 100) else withPaise.format(BigDecimal.valueOf(abs, 2))
        return sign(paise) + symbol + digits
    }

    /** Whole rupees, floored, so the app never shows more than is really left: 81 034 paise → "₹810". */
    @Synchronized
    fun formatWhole(paise: Long): String {
        val rupees = Math.floorDiv(paise, 100L)
        return sign(rupees) + symbol + whole.format(rupees.absoluteValue)
    }

    private fun sign(value: Long) = if (value < 0) "-" else ""
}

val LocalMoneyFormatter: ProvidableCompositionLocal<MoneyFormatter> = staticCompositionLocalOf { MoneyFormatter() }
