package com.shortsblocker.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchdogDecisionTest {
    @Test
    fun `master on and service enabled is Protected`() {
        assertEquals(
            WatchdogState.Protected,
            WatchdogDecision.state(masterEnabled = true, accessibilityServiceEnabled = true),
        )
    }

    @Test
    fun `master on and service disabled is Lockout`() {
        assertEquals(
            WatchdogState.Lockout,
            WatchdogDecision.state(masterEnabled = true, accessibilityServiceEnabled = false),
        )
    }

    @Test
    fun `master off is PausedByUser even if service enabled`() {
        assertEquals(
            WatchdogState.PausedByUser,
            WatchdogDecision.state(masterEnabled = false, accessibilityServiceEnabled = true),
        )
    }

    @Test
    fun `master off and service disabled is PausedByUser not Lockout`() {
        // The user legitimately paused via the PIN flow — no lock wall.
        assertEquals(
            WatchdogState.PausedByUser,
            WatchdogDecision.state(masterEnabled = false, accessibilityServiceEnabled = false),
        )
    }

    @Test
    fun `pin not set is PausedByUser even if service disabled`() {
        // During onboarding / fresh install before PIN is chosen, never show lock wall.
        assertEquals(
            WatchdogState.PausedByUser,
            WatchdogDecision.state(masterEnabled = true, accessibilityServiceEnabled = false, isPinSet = false),
        )
    }
}
