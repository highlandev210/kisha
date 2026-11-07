package com.example.kisha.allowance

object Money {
    /** ₹999,999,999.99 — the largest amount Kisha accepts. */
    const val MAX_PAISE: Long = 99_999_999_999L

    private val AMOUNT = Regex("""^(\d*)(?:\.(\d{0,2}))?$""")

    /**
     * Parses a user-typed rupee amount into paise. Grouping commas are ignored; at most two
     * decimals; no sign, symbol or exponent. Returns null for anything else or above [MAX_PAISE].
     */
    fun parsePaise(text: String): Long? {
        val cleaned = text.trim().replace(",", "")
        val match = AMOUNT.matchEntire(cleaned) ?: return null
        val whole = match.groupValues[1]
        val fraction = match.groupValues[2]
        if (whole.isEmpty() && fraction.isEmpty()) return null
        if (whole.length > 12) return null
        val rupees = if (whole.isEmpty()) 0L else whole.toLong()
        val paise = rupees * 100 + fraction.padEnd(2, '0').toLong()
        return paise.takeIf { it <= MAX_PAISE }
    }
}
