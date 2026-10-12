package com.shortsblocker.app.domain

enum class PermissionRequirement {
    ACCESSIBILITY,
    OVERLAY,
    NOTIFICATIONS,
    BATTERY_OPTIMIZATION,
}

data class ProtectionStatus(
    val isAccessibilityGranted: Boolean,
    val isOverlayGranted: Boolean,
    val isNotificationGranted: Boolean,
    val isBatteryExempted: Boolean,
    val isMasterBlockingEnabled: Boolean,
    val isPinSet: Boolean,
) {
    val isFullyProtected: Boolean
        get() = isAccessibilityGranted && isOverlayGranted && isMasterBlockingEnabled && isPinSet

    val missingRequiredPermissions: List<PermissionRequirement>
        get() = buildList {
            if (!isAccessibilityGranted) add(PermissionRequirement.ACCESSIBILITY)
            if (!isOverlayGranted) add(PermissionRequirement.OVERLAY)
        }
}
