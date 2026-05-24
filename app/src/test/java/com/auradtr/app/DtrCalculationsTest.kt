package com.auradtr.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class DtrCalculationsTest {

    @Test
    fun testStandardWorkedMinutesNoBreak() {
        val clockIn = Instant.parse("2026-05-23T08:00:00Z")
        val clockOut = Instant.parse("2026-05-23T17:00:00Z") // 9 hours
        
        val duration = Duration.between(clockIn, clockOut)
        val workedMinutes = duration.toMinutes().toInt()
        
        assertEquals(540, workedMinutes) // 9 hours * 60 minutes = 540 minutes
    }

    @Test
    fun testWorkedMinutesWithLunchBreak() {
        val clockIn = Instant.parse("2026-05-23T08:00:00Z")
        val lunchStart = Instant.parse("2026-05-23T12:00:00Z")
        val lunchEnd = Instant.parse("2026-05-23T13:00:00Z") // 1 hour break
        val clockOut = Instant.parse("2026-05-23T17:00:00Z") // 9 hours total elapsed
        
        val elapsed = Duration.between(clockIn, clockOut)
        val lunchDuration = Duration.between(lunchStart, lunchEnd)
        val workedMinutes = elapsed.minus(lunchDuration).toMinutes().toInt()
        
        assertEquals(480, workedMinutes) // 9 hours total elapsed - 1 hour break = 8 hours (480 minutes)
    }

    @Test
    fun testForgotLunchEndFallbackCalculation() {
        val clockIn = Instant.parse("2026-05-23T08:00:00Z")
        val lunchStart = Instant.parse("2026-05-23T12:00:00Z")
        val lunchEnd: Instant? = null // Trainee forgot to end break
        val clockOut = Instant.parse("2026-05-23T17:00:00Z")
        
        var elapsed = Duration.between(clockIn, clockOut)
        
        // Handle lunch break fallback
        if (lunchStart != null) {
            val actualLunchEnd = lunchEnd ?: lunchStart.plusSeconds(3600) // Fallback to 60-minute break
            val lunchDuration = Duration.between(lunchStart, actualLunchEnd)
            elapsed = elapsed.minus(lunchDuration)
        }
        
        val workedMinutes = elapsed.toMinutes().toInt()
        assertEquals(480, workedMinutes) // 9 hours total elapsed - 1 hour fallback break = 8 hours (480 minutes)
    }
}
