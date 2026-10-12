package com.shortsblocker.app.security

import android.content.Context
import android.content.SharedPreferences

/**
 * Tracks failed PIN entry attempts to protect against brute-force guessing.
 * Applies exponential backoff lockouts that survive app restarts.
 */
class PinAttemptTracker(
    private val prefs: SharedPreferences,
    private val timeProvider: () -> Long = { System.currentTimeMillis() },
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        { System.currentTimeMillis() },
    )

    fun failedAttempts(): Int = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)

    fun isLockedOut(): Boolean {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        return timeProvider() < lockoutUntil
    }

    fun remainingLockoutSeconds(): Long {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val diff = lockoutUntil - timeProvider()
        return if (diff > 0) (diff + 999) / 1000 else 0L
    }

    fun recordFailure(): Long {
        val attempts = failedAttempts() + 1
        val now = timeProvider()
        val lockoutSeconds = when {
            attempts < 5 -> 0L
            attempts == 5 -> 30L
            attempts == 6 -> 60L
            attempts == 7 -> 300L
            attempts == 8 -> 1800L
            else -> 3600L
        }

        val lockoutUntil = if (lockoutSeconds > 0) now + (lockoutSeconds * 1000) else 0L

        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, attempts)
            .putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
            .apply()

        return lockoutSeconds
    }

    fun recordSuccess() {
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "pin_attempt_tracker"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
    }
}
