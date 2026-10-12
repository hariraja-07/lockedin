package com.shortsblocker.app.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WatchdogMonitorTest {
    @Test
    fun `single lockout evaluation is debounced and does not trigger lockout`() =
        runTest {
            var evaluatedState: WatchdogState = WatchdogState.Protected
            val stateChanges = mutableListOf<WatchdogState>()

            val monitor =
                WatchdogMonitor(
                    currentState = { evaluatedState },
                    onStateChange = { stateChanges.add(it) },
                    pollIntervalMillis = 100L,
                    initialStartupDelayMillis = 0L,
                    lockoutDebounceCount = 2,
                )

            val job = launch { monitor.run() }

            // Initial poll: Protected
            advanceTimeBy(100L)
            assertEquals(listOf(WatchdogState.Protected), stateChanges)

            // First lockout evaluation (count = 1 < 2): should remain Protected
            evaluatedState = WatchdogState.Lockout
            advanceTimeBy(100L)
            assertEquals(listOf(WatchdogState.Protected), stateChanges)

            // Flips back to Protected before reaching threshold: lockout count resets
            evaluatedState = WatchdogState.Protected
            advanceTimeBy(100L)
            assertEquals(listOf(WatchdogState.Protected), stateChanges)

            job.cancelAndJoin()
        }

    @Test
    fun `two consecutive lockout evaluations trigger Lockout state`() =
        runTest {
            var evaluatedState: WatchdogState = WatchdogState.Protected
            val stateChanges = mutableListOf<WatchdogState>()

            val monitor =
                WatchdogMonitor(
                    currentState = { evaluatedState },
                    onStateChange = { stateChanges.add(it) },
                    pollIntervalMillis = 100L,
                    initialStartupDelayMillis = 0L,
                    lockoutDebounceCount = 2,
                )

            val job = launch { monitor.run() }

            // Initial poll: Protected
            advanceTimeBy(100L)
            assertEquals(listOf(WatchdogState.Protected), stateChanges)

            // First lockout poll (count = 1)
            evaluatedState = WatchdogState.Lockout
            advanceTimeBy(100L)
            assertEquals(listOf(WatchdogState.Protected), stateChanges)

            // Second lockout poll (count = 2): triggers Lockout
            advanceTimeBy(100L)
            assertEquals(listOf(WatchdogState.Protected, WatchdogState.Lockout), stateChanges)

            job.cancelAndJoin()
        }

    @Test
    fun `paused by user immediately transitions without debounce`() =
        runTest {
            var evaluatedState: WatchdogState = WatchdogState.Protected
            val stateChanges = mutableListOf<WatchdogState>()

            val monitor =
                WatchdogMonitor(
                    currentState = { evaluatedState },
                    onStateChange = { stateChanges.add(it) },
                    pollIntervalMillis = 100L,
                    initialStartupDelayMillis = 0L,
                    lockoutDebounceCount = 2,
                )

            val job = launch { monitor.run() }

            advanceTimeBy(100L)
            assertEquals(listOf(WatchdogState.Protected), stateChanges)

            evaluatedState = WatchdogState.PausedByUser
            advanceTimeBy(100L)
            assertEquals(listOf(WatchdogState.Protected, WatchdogState.PausedByUser), stateChanges)

            job.cancelAndJoin()
        }

    @Test
    fun `emits only when the state actually changes`() =
        runTest {
            val polledStates =
                ArrayDeque(
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

            val job =
                launch {
                    WatchdogMonitor(
                        currentState = { polledStates.removeFirstOrNull() ?: WatchdogState.Protected },
                        onStateChange = { emitted += it },
                        pollIntervalMillis = 100L,
                        lockoutDebounceCount = 1,
                    ).run()
                }
            advanceTimeBy(1000L)
            job.cancelAndJoin()

            assertEquals(
                listOf(WatchdogState.Protected, WatchdogState.Lockout, WatchdogState.PausedByUser, WatchdogState.Protected),
                emitted,
            )
        }

    @Test
    fun `no emission when state never changes`() =
        runTest {
            val emitted = mutableListOf<WatchdogState>()

            val job =
                launch {
                    WatchdogMonitor(
                        currentState = { WatchdogState.Protected },
                        onStateChange = { emitted += it },
                        pollIntervalMillis = 100L,
                    ).run()
                }
            advanceTimeBy(500L)
            job.cancelAndJoin()

            assertEquals(listOf(WatchdogState.Protected), emitted)
        }
}
