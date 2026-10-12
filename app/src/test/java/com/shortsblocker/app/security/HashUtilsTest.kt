package com.shortsblocker.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HashUtilsTest {
    @Test
    fun `same value and salt produce same hash`() {
        val salt = HashUtils.randomSalt()
        assertEquals(HashUtils.hash("1234", salt), HashUtils.hash("1234", salt))
    }

    @Test
    fun `different salts produce different hashes for the same value`() {
        val hashA = HashUtils.hash("1234", HashUtils.randomSalt())
        val hashB = HashUtils.hash("1234", HashUtils.randomSalt())
        assertNotEquals(hashA, hashB)
    }

    @Test
    fun `salt hex round trips`() {
        val salt = HashUtils.randomSalt()
        val roundTripped = HashUtils.saltFromHex(HashUtils.saltToHex(salt))
        assertEquals(salt.toList(), roundTripped.toList())
    }

    @Test
    fun `modern hash contains iteration prefix and verifies`() {
        val salt = HashUtils.randomSalt()
        val hash = HashUtils.hash("1234", salt)
        assertTrue(hash.startsWith("${HashUtils.DEFAULT_ITERATIONS}:"))
        assertTrue(HashUtils.verify("1234", salt, hash))
        assertFalse(HashUtils.verify("9999", salt, hash))
        assertFalse(HashUtils.needsUpgrade(hash))
    }

    @Test
    fun `legacy hash without prefix verifies with legacy iterations and needs upgrade`() {
        val salt = HashUtils.randomSalt()
        val legacyBareHash = HashUtils.rawHash("1234", salt, HashUtils.LEGACY_ITERATIONS)
        assertFalse(legacyBareHash.contains(':'))
        assertTrue(HashUtils.verify("1234", salt, legacyBareHash))
        assertFalse(HashUtils.verify("9999", salt, legacyBareHash))
        assertTrue(HashUtils.needsUpgrade(legacyBareHash))
    }
}
