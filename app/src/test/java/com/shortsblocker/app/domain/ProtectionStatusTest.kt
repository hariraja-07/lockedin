package com.shortsblocker.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionStatusTest {
    @Test
    fun `isFullyProtected returns true only when all required flags are true`() {
        val fullyProtected =
            ProtectionStatus(
                isAccessibilityGranted = true,
                isOverlayGranted = true,
                isNotificationGranted = true,
                isBatteryExempted = true,
                isMasterBlockingEnabled = true,
                isPinSet = true,
            )
        assertTrue(fullyProtected.isFullyProtected)
        assertTrue(fullyProtected.missingRequiredPermissions.isEmpty())
    }

    @Test
    fun `missingRequiredPermissions identifies missing accessibility and overlay`() {
        val missingBoth =
            ProtectionStatus(
                isAccessibilityGranted = false,
                isOverlayGranted = false,
                isNotificationGranted = true,
                isBatteryExempted = false,
                isMasterBlockingEnabled = true,
                isPinSet = true,
            )
        assertFalse(missingBoth.isFullyProtected)
        val missing = missingBoth.missingRequiredPermissions
        assertEquals(2, missing.size)
        assertTrue(missing.contains(PermissionRequirement.ACCESSIBILITY))
        assertTrue(missing.contains(PermissionRequirement.OVERLAY))
    }

    @Test
    fun `pin not set prevents fully protected status`() {
        val noPin =
            ProtectionStatus(
                isAccessibilityGranted = true,
                isOverlayGranted = true,
                isNotificationGranted = true,
                isBatteryExempted = true,
                isMasterBlockingEnabled = true,
                isPinSet = false,
            )
        assertFalse(noPin.isFullyProtected)
    }
}
