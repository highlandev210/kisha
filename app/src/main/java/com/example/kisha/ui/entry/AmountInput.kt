package com.example.kisha.ui.entry

import com.example.kisha.allowance.Money

sealed interface Key {
    data class Digit(val value: Int) : Key
    data object Dot : Key
    data object Backspace : Key
}

/** What the quick-entry keypad has typed so far, e.g. "120" or "45.5". */
data class AmountInput(val text: String = "") {
    val paise: Long get() = Money.parsePaise(text) ?: 0

    val canSave: Boolean get() = paise > 0

    fun press(key: Key): AmountInput = when (key) {
        Key.Backspace -> copy(text = text.dropLast(1))
        Key.Dot -> when {
            '.' in text -> this
            text.isEmpty() -> copy(text = "0.")
            else -> copy(text = "$text.")
        }
        is Key.Digit -> {
            val decimals = text.substringAfter('.', missingDelimiterValue = "")
            val candidate = when {
                text == "0" -> key.value.toString()
                '.' in text && decimals.length >= 2 -> null
                else -> text + key.value
            }
            if (candidate == null || Money.parsePaise(candidate) == null) this else copy(text = candidate)
        }
    }

    companion object {
        fun fromPaise(paise: Long): AmountInput {
            val rupees = paise / 100
            val rest = paise % 100
            return AmountInput(if (rest == 0L) "$rupees" else "$rupees.${rest.toString().padStart(2, '0')}")
        }
    }
}
