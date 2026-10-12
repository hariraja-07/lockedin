package com.shortsblocker.app.watchdog

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

class WatchdogMonitorTest {

    @Test
    fun `emits only when the state actually changes`() = runBlocking {
        val polledStates = ArrayDeque(
            listOf(
                WatchdogState.Protected,
                WatchdogState.Protected,
                WatchdogState.Lockout,
                WatchdogState.Lockout,
                WatchdogState.Lockout,
                WatchdogState.PausedByUser,
                WatchdogState.Protected,
            ),
        )
        val emitted = mutableListOf<WatchdogState>()

        val job = launch {
            WatchdogMonitor(
                currentState = { polledStates.removeFirstOrNull() ?: WatchdogState.Protected },
                onStateChange = { emitted += it },
                pollIntervalMillis = 1L,
            ).run()
        }
        while (polledStates.isNotEmpty()) delay(5L)
        delay(20L)
        job.cancel()

        assertEquals(
            listOf(WatchdogState.Protected, WatchdogState.Lockout, WatchdogState.PausedByUser, WatchdogState.Protected),
            emitted,
        )
    }

    @Test
    fun `no emission when state never changes`() = runBlocking {
        val emitted = mutableListOf<WatchdogState>()

        val job = launch {
            WatchdogMonitor(
                currentState = { WatchdogState.Protected },
                onStateChange = { emitted += it },
                pollIntervalMillis = 1L,
            ).run()
        }
        delay(20L)
        job.cancel()

        assertEquals(listOf(WatchdogState.Protected), emitted)
    }
}
