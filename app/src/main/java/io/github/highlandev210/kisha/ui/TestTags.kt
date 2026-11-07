package io.github.highlandev210.kisha.ui

/** Stable semantics tags for UI tests. */
object TestTags {
    const val ALLOWANCE = "allowance"
    const val PREVIEW_ALLOWANCE = "previewAllowance"
    const val INCOME_FIELD = "incomeField"
    const val FIXED_NAME_FIELD = "fixedNameField"
    const val FIXED_AMOUNT_FIELD = "fixedAmountField"
    const val ENTRY_SAVE = "entrySave"
    const val KEY_DOT = "keyDot"
    const val KEY_BACKSPACE = "keyBackspace"

    fun key(digit: Int) = "key$digit"
}
