package com.shortsblocker.app.security

import com.shortsblocker.app.data.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PinManagerTest {
    private lateinit var storage: InMemorySecureStorage
    private lateinit var trackerPrefs: FakeSharedPreferences
    private lateinit var recoveryTracker: PinRecoveryTracker
    private lateinit var pinManager: PinManager

    @Before
    fun setUp() {
        storage = InMemorySecureStorage()
        trackerPrefs = FakeSharedPreferences()
        recoveryTracker = PinRecoveryTracker(trackerPrefs, timeProvider = { 1_000_000L })
        pinManager = PinManager(storage, recoveryTracker)
    }

    @Test
    fun `initially no PIN is set and no recovery is set`() {
        assertFalse(pinManager.isPinSet())
        assertFalse(pinManager.hasRecovery())
        assertNull(pinManager.getRecoveryQuestion())
        assertFalse(pinManager.keystoreCorrupted)
    }

    @Test
    fun `setPin and verifyPin with correct PIN returns true`() {
        pinManager.setPin("1234")
        assertTrue(pinManager.isPinSet())
        assertTrue(pinManager.verifyPin("1234"))
        assertFalse(pinManager.verifyPin("9999"))
        assertFalse(pinManager.verifyPin("123"))
    }

    @Test
    fun `transparent upgrade of old hash format`() {
        val legacySalt = HashUtils.randomSalt()
        val legacyHash = HashUtils.hash(value = "5678", salt = legacySalt, iterations = 10_000)

        storage.putString("pin_salt", HashUtils.saltToHex(legacySalt))
        storage.putString("pin_hash", legacyHash)

        assertTrue(pinManager.isPinSet())
        assertTrue(HashUtils.needsUpgrade(legacyHash))

        // Verifying PIN triggers upgrade
        val verified = pinManager.verifyPin("5678")
        assertTrue(verified)

        val updatedHash = storage.getString("pin_hash")!!
        assertFalse(HashUtils.needsUpgrade(updatedHash))
        assertTrue(pinManager.verifyPin("5678"))
    }

    @Test
    fun `setRecovery and verifyRecoveryAnswer normalizes case and whitespace`() {
        pinManager.setRecovery("Mother's maiden name?", "  Smith  ")
        assertTrue(pinManager.hasRecovery())
        assertEquals("Mother's maiden name?", pinManager.getRecoveryQuestion())

        assertTrue(pinManager.verifyRecoveryAnswer("smith"))
        assertTrue(pinManager.verifyRecoveryAnswer("SMITH"))
        assertTrue(pinManager.verifyRecoveryAnswer("  Smith  "))
        assertFalse(pinManager.verifyRecoveryAnswer("jones"))
    }

    @Test
    fun `wrong recovery answers trigger lockout after 5 attempts`() {
        pinManager.setRecovery("City?", "London")

        for (i in 1..4) {
            assertFalse(pinManager.verifyRecoveryAnswer("Paris"))
            assertFalse(pinManager.isRecoveryLockedOut())
        }

        assertFalse(pinManager.verifyRecoveryAnswer("Paris"))
        assertTrue(pinManager.isRecoveryLockedOut())

        // Even correct answer rejected during lockout
        assertFalse(pinManager.verifyRecoveryAnswer("London"))
    }

    @Test
    fun `resetCorruptedStorage clears all data`() {
        pinManager.setPin("1234")
        pinManager.setRecovery("Pet?", "Rover")
        assertTrue(pinManager.isPinSet())
        assertTrue(pinManager.hasRecovery())

        val resetSuccess = pinManager.resetCorruptedStorage()
        assertTrue(resetSuccess)
        assertFalse(pinManager.isPinSet())
        assertFalse(pinManager.hasRecovery())
        assertNull(pinManager.getRecoveryQuestion())
    }
}
