package com.shortsblocker.app.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * PIN + recovery-question storage. Everything is salted-hashed on device;
 * nothing is ever sent off the phone. Recovery exists (per product decision)
 * so a forgotten PIN doesn't permanently lock the owner out of Settings —
 * the recovery answer is the only way to reset it, no email/SMS involved.
 */
class PinManager(private val context: Context) {

    var keystoreCorrupted: Boolean = false
        private set

    private var prefs: SharedPreferences = initPrefs()

    private fun initPrefs(): SharedPreferences {
        return runCatching {
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
        }.getOrElse { error ->
            Log.e("PinManager", "Failed to initialize EncryptedSharedPreferences (Keystore failure)", error)
            keystoreCorrupted = true
            context.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
        }
    }

    fun isPinSet(): Boolean = runCatching {
        prefs.contains(KEY_PIN_HASH)
    }.getOrElse {
        keystoreCorrupted = true
        false
    }

    fun setPin(pin: String) {
        val salt = HashUtils.randomSalt()
        runCatching {
            prefs.edit()
                .putString(KEY_PIN_SALT, HashUtils.saltToHex(salt))
                .putString(KEY_PIN_HASH, HashUtils.hash(pin, salt))
                .apply()
        }.onFailure {
            keystoreCorrupted = true
            Log.e("PinManager", "Failed to set PIN", it)
        }
    }

    fun verifyPin(pin: String): Boolean = runCatching {
        val saltHex = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = HashUtils.saltFromHex(saltHex)
        val valid = HashUtils.verify(pin, salt, storedHash)
        if (valid && HashUtils.needsUpgrade(storedHash)) {
            val newSalt = HashUtils.randomSalt()
            prefs.edit()
                .putString(KEY_PIN_SALT, HashUtils.saltToHex(newSalt))
                .putString(KEY_PIN_HASH, HashUtils.hash(pin, newSalt))
                .apply()
        }
        valid
    }.getOrElse {
        keystoreCorrupted = true
        false
    }

    fun setRecovery(question: String, answer: String) {
        val salt = HashUtils.randomSalt()
        runCatching {
            prefs.edit()
                .putString(KEY_RECOVERY_QUESTION, question)
                .putString(KEY_RECOVERY_SALT, HashUtils.saltToHex(salt))
                .putString(KEY_RECOVERY_HASH, HashUtils.hash(answer.trim().lowercase(), salt))
                .apply()
        }.onFailure {
            keystoreCorrupted = true
            Log.e("PinManager", "Failed to set recovery", it)
        }
    }

    fun getRecoveryQuestion(): String? = runCatching {
        prefs.getString(KEY_RECOVERY_QUESTION, null)
    }.getOrElse {
        keystoreCorrupted = true
        null
    }

    fun hasRecovery(): Boolean = runCatching {
        prefs.contains(KEY_RECOVERY_HASH)
    }.getOrElse {
        keystoreCorrupted = true
        false
    }

    fun verifyRecoveryAnswer(answer: String): Boolean = runCatching {
        val saltHex = prefs.getString(KEY_RECOVERY_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_RECOVERY_HASH, null) ?: return false
        val salt = HashUtils.saltFromHex(saltHex)
        val normalizedAnswer = answer.trim().lowercase()
        val valid = HashUtils.verify(normalizedAnswer, salt, storedHash)
        if (valid && HashUtils.needsUpgrade(storedHash)) {
            val newSalt = HashUtils.randomSalt()
            prefs.edit()
                .putString(KEY_RECOVERY_SALT, HashUtils.saltToHex(newSalt))
                .putString(KEY_RECOVERY_HASH, HashUtils.hash(normalizedAnswer, newSalt))
                .apply()
        }
        valid
    }.getOrElse {
        keystoreCorrupted = true
        false
    }

    fun resetCorruptedStorage(): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(PREFS_NAME)
            } else {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
            }
            prefs = initPrefs()
            keystoreCorrupted = false
            true
        }.getOrElse {
            Log.e("PinManager", "Failed to reset corrupted storage", it)
            false
        }
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
