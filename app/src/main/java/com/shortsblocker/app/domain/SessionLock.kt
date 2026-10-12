package com.shortsblocker.app.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks session unlock state for PIN-protected areas (Settings).
 * Automatically locks the session when backgrounded for longer than LOCK_TIMEOUT_MS.
 */
class SessionLock(
    private val timeProvider: () -> Long = { System.currentTimeMillis() },
    private val timeoutMs: Long = LOCK_TIMEOUT_MS,
) {
    private val _unlocked = MutableStateFlow(false)
    val unlockedFlow: StateFlow<Boolean> = _unlocked.asStateFlow()

    private var backgroundTimestamp: Long = 0L

    fun isUnlocked(): Boolean = _unlocked.value

    fun unlock() {
        _unlocked.value = true
        backgroundTimestamp = 0L
    }

    fun lock() {
        _unlocked.value = false
        backgroundTimestamp = 0L
    }

    fun onAppBackgrounded() {
        if (_unlocked.value) {
            backgroundTimestamp = timeProvider()
        }
    }

    fun onAppForegrounded() {
        if (_unlocked.value && backgroundTimestamp > 0L) {
            val elapsed = timeProvider() - backgroundTimestamp
            if (elapsed >= timeoutMs) {
                lock()
            } else {
                backgroundTimestamp = 0L
            }
        }
    }

    companion object {
        const val LOCK_TIMEOUT_MS: Long = 30_000L // 30 seconds
    }
}
