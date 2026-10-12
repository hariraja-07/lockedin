package com.shortsblocker.app.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Modern Android Keystore-backed AES-256-GCM encrypted storage.
 * Stores authenticated ciphertexts in private [SharedPreferences] and maintains
 * an in-memory decrypted cache for instantaneous, zero-latency synchronous reads.
 * Replaces deprecated [androidx.security.crypto.EncryptedSharedPreferences].
 */
class KeystoreEncryptedStorage(
    context: Context,
    prefsName: String = DEFAULT_PREFS_NAME,
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
) : SecureStorage {
    private val prefs: SharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    private val secretKey: SecretKey = getOrCreateSecretKey(keyAlias)
    private val cache = ConcurrentHashMap<String, String>()

    init {
        loadAndDecryptAll()
    }

    private fun loadAndDecryptAll() {
        val allEntries = prefs.all
        for ((key, rawValue) in allEntries) {
            if (rawValue is String) {
                val decrypted = decrypt(rawValue)
                cache[key] = decrypted
            }
        }
    }

    private fun getOrCreateSecretKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        if (!keyStore.containsAlias(alias)) {
            val keyGenerator =
                KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEY_STORE,
                )
            val parameterSpec =
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()

            keyGenerator.init(parameterSpec)
            keyGenerator.generateKey()
        }
        val entry =
            keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
                ?: throw IllegalStateException("Key alias '$alias' found, but entry is not SecretKeyEntry")
        return entry.secretKey
    }

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return Base64.getEncoder().encodeToString(combined)
    }

    private fun decrypt(encryptedBase64: String): String {
        val combined = Base64.getDecoder().decode(encryptedBase64)
        if (combined.size < GCM_IV_LENGTH_BYTES) {
            throw IllegalArgumentException("Encrypted payload too short for GCM IV")
        }
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    override fun getString(key: String): String? = cache[key]

    override fun putString(
        key: String,
        value: String,
    ) {
        val encrypted = encrypt(value)
        cache[key] = value
        prefs.edit().putString(key, encrypted).apply()
    }

    override fun contains(key: String): Boolean = cache.containsKey(key)

    override fun remove(key: String) {
        cache.remove(key)
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        cache.clear()
        prefs.edit().clear().apply()
    }

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        const val DEFAULT_PREFS_NAME = "keystore_pin_store"
        const val DEFAULT_KEY_ALIAS = "lockedin_pin_cipher_key"

        fun deleteKey(keyAlias: String = DEFAULT_KEY_ALIAS) {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            if (keyStore.containsAlias(keyAlias)) {
                keyStore.deleteEntry(keyAlias)
            }
        }
    }
}
