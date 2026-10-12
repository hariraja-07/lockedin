package com.shortsblocker.app.security

import com.shortsblocker.app.data.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinRecoveryCooldownTest {

    private var currentTime = 1_000_000L
    private val prefs = FakeSharedPreferences()
    private val tracker = PinRecoveryTracker(prefs, timeProvider = { currentTime })

    @Test
    fun `less than 5 failures does not lock out`() {
        for (i in 1..4) {
            val locked = tracker.recordFailure()
            assertFalse(locked)
            assertFalse(tracker.isRecoveryLockedOut())
        }
        assertEquals(4, tracker.failedAttempts())
    }

    @Test
    fun `fifth failure triggers 24 hour lockout`() {
        for (i in 1..4) {
            tracker.recordFailure()
        }
        val locked = tracker.recordFailure()
        assertTrue(locked)
        assertTrue(tracker.isRecoveryLockedOut())
        assertEquals(86400L, tracker.remainingCooldownSeconds())

        // 12 hours later: still locked out
        currentTime += 12 * 3600 * 1000L
        assertTrue(tracker.isRecoveryLockedOut())
        assertEquals(43200L, tracker.remainingCooldownSeconds())

        // 24 hours later: expired
        currentTime += 12 * 3600 * 1000L
        assertFalse(tracker.isRecoveryLockedOut())
        assertEquals(0L, tracker.remainingCooldownSeconds())
    }

    @Test
    fun `lockout survives tracker re-creation`() {
        for (i in 1..5) {
            tracker.recordFailure()
        }
        assertTrue(tracker.isRecoveryLockedOut())

        // Recreate tracker pointing to same prefs
        val newTracker = PinRecoveryTracker(prefs, timeProvider = { currentTime })
        assertTrue(newTracker.isRecoveryLockedOut())
        assertEquals(5, newTracker.failedAttempts())
    }

    @Test
    fun `clear recovery failures resets lockout and attempts`() {
        for (i in 1..5) {
            tracker.recordFailure()
        }
        assertTrue(tracker.isRecoveryLockedOut())

        tracker.clearRecoveryFailures()
        assertFalse(tracker.isRecoveryLockedOut())
        assertEquals(0, tracker.failedAttempts())
        assertEquals(0L, tracker.remainingCooldownSeconds())
    }
}
