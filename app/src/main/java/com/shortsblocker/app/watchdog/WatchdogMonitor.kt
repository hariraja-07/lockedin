package com.shortsblocker.app.watchdog

import kotlinx.coroutines.delay

/**
 * Polls the current [WatchdogState] on a fixed interval and invokes
 * [onStateChange] only when the state actually changes. The polling loop is a
 * suspend function so the caller controls the scope and dispatcher.
 */
class WatchdogMonitor(
    private val currentState: () -> WatchdogState,
    private val onStateChange: (WatchdogState) -> Unit,
    private val pollIntervalMillis: Long = DEFAULT_POLL_INTERVAL_MILLIS,
    private val initialStartupDelayMillis: Long = 0L,
    private val lockoutDebounceCount: Int = DEFAULT_LOCKOUT_DEBOUNCE_COUNT,
) {

    suspend fun run() {
        if (initialStartupDelayMillis > 0L) {
            delay(initialStartupDelayMillis)
        }
        var activeState: WatchdogState? = null
        var consecutiveLockoutCount = 0

        while (true) {
            val evaluated = currentState()

            val targetState = if (evaluated == WatchdogState.Lockout) {
                consecutiveLockoutCount++
                if (consecutiveLockoutCount >= lockoutDebounceCount) {
                    WatchdogState.Lockout
                } else {
                    activeState ?: WatchdogState.Protected
                }
            } else {
                consecutiveLockoutCount = 0
                evaluated
            }

            if (targetState != activeState) {
                activeState = targetState
                onStateChange(targetState)
            }
            delay(pollIntervalMillis)
        }
    }

    companion object {
        const val DEFAULT_POLL_INTERVAL_MILLIS = 3_000L
        const val DEFAULT_LOCKOUT_DEBOUNCE_COUNT = 2
    }
}
