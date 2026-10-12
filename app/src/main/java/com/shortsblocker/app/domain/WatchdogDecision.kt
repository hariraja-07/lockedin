package com.shortsblocker.app.domain

/**
 * Pure decision logic for the watchdog. Given the two inputs it cares about,
 * decides what the enforcement layer should do. Kept framework-free so it is
 * unit-testable on the JVM without a device.
 */
sealed interface WatchdogState {
    /** Master blocking is on and the accessibility service is enabled. Nothing to do. */
    data object Protected : WatchdogState

    /**
     * The user turned master blocking off through the app's PIN-gated flow.
     * That is a legitimate pause, not a bypass — no lock wall is shown.
     */
    data object PausedByUser : WatchdogState

    /**
     * Master blocking is on (so the user has not legitimately paused it), but the
     * accessibility service itself has been disabled in system Settings — the
     * lock wall must appear until protection is restored.
     */
    data object Lockout : WatchdogState
}

object WatchdogDecision {
    fun state(
        masterEnabled: Boolean,
        accessibilityServiceEnabled: Boolean,
        isPinSet: Boolean = true,
    ): WatchdogState = when {
        !masterEnabled -> WatchdogState.PausedByUser
        !isPinSet -> WatchdogState.PausedByUser
        accessibilityServiceEnabled -> WatchdogState.Protected
        else -> WatchdogState.Lockout
    }
}
