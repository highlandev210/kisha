package io.github.highlandev210.kisha.ui.entry

import io.github.highlandev210.kisha.allowance.AllowanceEngine
import io.github.highlandev210.kisha.allowance.AllowanceStatus
import io.github.highlandev210.kisha.allowance.DayAllowance

/** What today will look like if the expense being typed is saved. */
data class Projection(val remainingPaise: Long, val status: AllowanceStatus)

fun project(today: DayAllowance, typedPaise: Long): Projection {
    val remaining = today.remainingPaise - typedPaise
    return Projection(remaining, AllowanceEngine.status(today.basePaise, remaining))
}
