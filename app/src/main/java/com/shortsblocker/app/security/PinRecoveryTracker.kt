package com.shortsblocker.app.security

import android.content.Context
import android.content.SharedPreferences

/**
 * Tracks failed recovery answer attempts to prevent brute-forcing.
 * After 5 consecutive failures, a 24-hour lockout is enforced.
 */
class PinRecoveryTracker(
    private val prefs: SharedPreferences,
    private val timeProvider: () -> Long = { System.currentTimeMillis() },
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        { System.currentTimeMillis() },
    )

    fun failedAttempts(): Int = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)

    fun isRecoveryLockedOut(): Boolean {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        return timeProvider() < lockoutUntil
    }

    fun remainingCooldownSeconds(): Long {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val diff = lockoutUntil - timeProvider()
        return if (diff > 0) (diff + 999) / 1000 else 0L
    }

    fun recoveryLockedOutUntilMillis(): Long = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)

    fun recordFailure(): Boolean {
        val attempts = failedAttempts() + 1
        val now = timeProvider()
        val isLocked = attempts >= MAX_ATTEMPTS
        val lockoutUntil = if (isLocked) now + COOLDOWN_MILLIS else 0L

        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, attempts)
            .putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
            .apply()

        return isLocked
    }

    fun clearRecoveryFailures() {
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "pin_recovery_tracker"
        private const val KEY_FAILED_ATTEMPTS = "recovery_failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "recovery_lockout_until"
        const val MAX_ATTEMPTS = 5
        const val COOLDOWN_MILLIS = 24 * 60 * 60 * 1000L // 24 hours
    }
}
