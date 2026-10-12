package com.shortsblocker.app.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Salted PBKDF2 hashing so PIN and recovery answer are never stored in plaintext. */
object HashUtils {

    const val DEFAULT_ITERATIONS = 310_000
    const val LEGACY_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256

    fun randomSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun hash(value: String, salt: ByteArray, iterations: Int = DEFAULT_ITERATIONS): String {
        val raw = rawHash(value, salt, iterations)
        return "$iterations:$raw"
    }

    fun rawHash(value: String, salt: ByteArray, iterations: Int): String {
        val spec = PBEKeySpec(value.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val digest = factory.generateSecret(spec).encoded
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Constant-time verification supporting both modern "$iterations:$hash"
     * and legacy bare "$hash" strings.
     */
    fun verify(input: String, salt: ByteArray, storedHashString: String): Boolean {
        val (iterations, expectedHex) = if (storedHashString.contains(':')) {
            val parts = storedHashString.split(':', limit = 2)
            parts[0].toIntOrNull() to parts[1]
        } else {
            LEGACY_ITERATIONS to storedHashString
        }

        if (iterations == null) return false

        val computedHex = rawHash(input, salt, iterations)
        return MessageDigest.isEqual(
            computedHex.toByteArray(Charsets.UTF_8),
            expectedHex.toByteArray(Charsets.UTF_8),
        )
    }

    fun needsUpgrade(storedHashString: String): Boolean {
        return !storedHashString.startsWith("$DEFAULT_ITERATIONS:")
    }

    fun saltToHex(salt: ByteArray): String = salt.joinToString("") { "%02x".format(it) }

    fun saltFromHex(hex: String): ByteArray =
        ByteArray(hex.length / 2) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
}
