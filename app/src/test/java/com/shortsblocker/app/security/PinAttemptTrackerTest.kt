package com.shortsblocker.app.security

import com.shortsblocker.app.logging.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinAttemptTrackerTest {

    private var currentTime = 100_000L
    private val prefs = FakeSharedPreferences()
    private val tracker = PinAttemptTracker(prefs, timeProvider = { currentTime })

    @Test
    fun `less than 5 failures does not lock out`() {
        for (i in 1..4) {
            val wait = tracker.recordFailure()
            assertEquals(0L, wait)
            assertFalse(tracker.isLockedOut())
        }
        assertEquals(4, tracker.failedAttempts())
    }

    @Test
    fun `fifth failure locks out for 30 seconds`() {
        for (i in 1..4) {
            tracker.recordFailure()
        }
        val wait = tracker.recordFailure()
        assertEquals(30L, wait)
        assertTrue(tracker.isLockedOut())
        assertEquals(30L, tracker.remainingLockoutSeconds())

        // Advance time 30 seconds
        currentTime += 30_000L
        assertFalse(tracker.isLockedOut())
        assertEquals(0L, tracker.remainingLockoutSeconds())
    }

    @Test
    fun `sixth failure locks out for 60 seconds`() {
        for (i in 1..5) {
            tracker.recordFailure()
        }
        val wait = tracker.recordFailure()
        assertEquals(60L, wait)
        assertTrue(tracker.isLockedOut())
    }

    @Test
    fun `success clears failures and lockout`() {
        for (i in 1..5) {
            tracker.recordFailure()
        }
        assertTrue(tracker.isLockedOut())

        tracker.recordSuccess()
        assertFalse(tracker.isLockedOut())
        assertEquals(0, tracker.failedAttempts())
        assertEquals(0L, tracker.remainingLockoutSeconds())
    }
}
