package com.example.kisha.data

import com.example.kisha.allowance.localDateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The current instant and zone. Read the zone on every call: it changes when the user travels (BP-4). */
interface TimeSource {
    fun now(): Instant
    fun zone(): ZoneId
}

fun TimeSource.today(): LocalDate = now().localDateIn(zone())

object SystemTimeSource : TimeSource {
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
