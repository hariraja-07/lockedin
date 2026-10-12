package com.shortsblocker.app.security

import android.content.SharedPreferences

/**
 * Interface abstraction for secure key-value string storage.
 * Decouples PIN/recovery storage from concrete crypto/storage implementations,
 * allowing hardware-backed Keystore storage in production and in-memory mocks in unit tests.
 */
interface SecureStorage {
    fun getString(key: String): String?

    fun putString(
        key: String,
        value: String,
    )

    fun contains(key: String): Boolean

    fun remove(key: String)

    fun clear()
}

/**
 * Pure in-memory implementation of [SecureStorage] for unit testing.
 */
class InMemorySecureStorage : SecureStorage {
    private val map = mutableMapOf<String, String>()

    override fun getString(key: String): String? = map[key]

    override fun putString(
        key: String,
        value: String,
    ) {
        map[key] = value
    }

    override fun contains(key: String): Boolean = map.containsKey(key)

    override fun remove(key: String) {
        map.remove(key)
    }

    override fun clear() {
        map.clear()
    }
}

/**
 * Fallback implementation using standard [SharedPreferences] when AndroidKeyStore
 * is unavailable or corrupted.
 */
class FallbackSecureStorage(
    private val prefs: SharedPreferences,
) : SecureStorage {
    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(
        key: String,
        value: String,
    ) {
        prefs.edit().putString(key, value).apply()
    }

    override fun contains(key: String): Boolean = prefs.contains(key)

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}
