package com.shortsblocker.app.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * PIN + recovery-question storage. Everything is salted-hashed on device;
 * nothing is ever sent off the phone. Recovery exists (per product decision)
 * so a forgotten PIN doesn't permanently lock the owner out of Settings —
 * the recovery answer is the only way to reset it, no email/SMS involved.
 */
class PinManager(context: Context) {

    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun isPinSet(): Boolean = prefs.contains(KEY_PIN_HASH)

    fun setPin(pin: String) {
        val salt = HashUtils.randomSalt()
        prefs.edit()
            .putString(KEY_PIN_SALT, HashUtils.saltToHex(salt))
            .putString(KEY_PIN_HASH, HashUtils.hash(pin, salt))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saltHex = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return HashUtils.hash(pin, HashUtils.saltFromHex(saltHex)) == storedHash
    }

    fun setRecovery(question: String, answer: String) {
        val salt = HashUtils.randomSalt()
        prefs.edit()
            .putString(KEY_RECOVERY_QUESTION, question)
            .putString(KEY_RECOVERY_SALT, HashUtils.saltToHex(salt))
            .putString(KEY_RECOVERY_HASH, HashUtils.hash(answer.trim().lowercase(), salt))
            .apply()
    }

    fun getRecoveryQuestion(): String? = prefs.getString(KEY_RECOVERY_QUESTION, null)

    fun hasRecovery(): Boolean = prefs.contains(KEY_RECOVERY_HASH)

    fun verifyRecoveryAnswer(answer: String): Boolean {
        val saltHex = prefs.getString(KEY_RECOVERY_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_RECOVERY_HASH, null) ?: return false
        return HashUtils.hash(answer.trim().lowercase(), HashUtils.saltFromHex(saltHex)) == storedHash
    }

    companion object {
        private const val PREFS_NAME = "pin_store"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_RECOVERY_QUESTION = "recovery_question"
        private const val KEY_RECOVERY_HASH = "recovery_hash"
        private const val KEY_RECOVERY_SALT = "recovery_salt"
    }
}
