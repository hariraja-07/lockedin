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
) {

    suspend fun run() {
        var last: WatchdogState? = null
        while (true) {
            val state = currentState()
            if (state != last) {
                last = state
                onStateChange(state)
            }
            delay(pollIntervalMillis)
        }
    }

    companion object {
        const val DEFAULT_POLL_INTERVAL_MILLIS = 3_000L
    }
}
