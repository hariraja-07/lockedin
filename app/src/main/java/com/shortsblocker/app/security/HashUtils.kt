package com.shortsblocker.app.security

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Salted PBKDF2 hashing so PIN and recovery answer are never stored in plaintext. */
object HashUtils {

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256

    fun randomSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun hash(value: String, salt: ByteArray): String {
        val spec = PBEKeySpec(value.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val digest = factory.generateSecret(spec).encoded
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun saltToHex(salt: ByteArray): String = salt.joinToString("") { "%02x".format(it) }

    fun saltFromHex(hex: String): ByteArray =
        ByteArray(hex.length / 2) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
}
