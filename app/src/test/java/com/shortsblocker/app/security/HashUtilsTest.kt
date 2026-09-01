package com.shortsblocker.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
}
