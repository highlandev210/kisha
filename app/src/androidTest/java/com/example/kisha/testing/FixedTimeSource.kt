package com.example.kisha.testing

import com.example.kisha.data.TimeSource
import java.time.Instant
import java.time.ZoneId

/** A controllable clock for tests: change [currentInstant] or [currentZone] to move time or travel. */
class FixedTimeSource(
    var currentInstant: Instant,
    var currentZone: ZoneId = ZoneId.of("Asia/Kolkata"),
) : TimeSource {
    override fun now(): Instant = currentInstant
    override fun zone(): ZoneId = currentZone
}
