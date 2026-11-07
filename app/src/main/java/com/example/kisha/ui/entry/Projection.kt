package com.example.kisha.ui.entry

import com.example.kisha.allowance.AllowanceEngine
import com.example.kisha.allowance.AllowanceStatus
import com.example.kisha.allowance.DayAllowance

/** What today will look like if the expense being typed is saved. */
data class Projection(val remainingPaise: Long, val status: AllowanceStatus)

fun project(today: DayAllowance, typedPaise: Long): Projection {
    val remaining = today.remainingPaise - typedPaise
    return Projection(remaining, AllowanceEngine.status(today.basePaise, remaining))
}
