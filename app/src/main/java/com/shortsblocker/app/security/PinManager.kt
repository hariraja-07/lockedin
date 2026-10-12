package com.shortsblocker.app.security

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * PIN + recovery-question storage. Everything is salted-hashed on device;
 * nothing is ever sent off the phone. Recovery exists (per product decision)
 * so a forgotten PIN doesn't permanently lock the owner out of Settings —
 * the recovery answer is the only way to reset it, no email/SMS involved.
 *
 * Backed by [KeystoreEncryptedStorage] (AndroidKeyStore AES-256-GCM) with transparent
 * one-time migration from legacy [EncryptedSharedPreferences].
 */
class PinManager internal constructor(
    private val context: Context?,
    customStorage: SecureStorage?,
    private val recoveryTracker: PinRecoveryTracker,
) {
    var keystoreCorrupted: Boolean = false
        private set

    private var storage: SecureStorage = customStorage ?: initStorage()

    constructor(
        context: Context,
        recoveryTracker: PinRecoveryTracker = PinRecoveryTracker(context),
    ) : this(
        context = context,
        customStorage = null,
        recoveryTracker = recoveryTracker,
    )

    internal constructor(
        storage: SecureStorage,
        recoveryTracker: PinRecoveryTracker,
    ) : this(
        context = null,
        customStorage = storage,
        recoveryTracker = recoveryTracker,
    )

    private fun initStorage(): SecureStorage {
        val ctx = context ?: return InMemorySecureStorage()
        return runCatching {
            val keyStorage = KeystoreEncryptedStorage(ctx)
            migrateLegacyIfPresent(ctx, keyStorage)
            keyStorage
        }.getOrElse { error ->
            Log.e("PinManager", "Failed to initialize KeystoreEncryptedStorage (Keystore failure)", error)
            keystoreCorrupted = true
            FallbackSecureStorage(
                ctx.getSharedPreferences(
                    "${KeystoreEncryptedStorage.DEFAULT_PREFS_NAME}_fallback",
                    Context.MODE_PRIVATE,
                ),
            )
        }
    }

    private fun migrateLegacyIfPresent(
        ctx: Context,
        target: SecureStorage,
    ) {
        if (target.contains(KEY_PIN_HASH)) {
            return
        }

        // Attempt 1: Check legacy EncryptedSharedPreferences ("pin_store")
        runCatching {
            val masterKey =
                MasterKey.Builder(ctx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            val legacy =
                EncryptedSharedPreferences.create(
                    ctx,
                    LEGACY_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            if (legacy.contains(KEY_PIN_HASH)) {
                val pinHash = legacy.getString(KEY_PIN_HASH, null)
                val pinSalt = legacy.getString(KEY_PIN_SALT, null)
                val reqQ = legacy.getString(KEY_RECOVERY_QUESTION, null)
                val recH = legacy.getString(KEY_RECOVERY_HASH, null)
                val recS = legacy.getString(KEY_RECOVERY_SALT, null)
                if (!pinHash.isNullOrEmpty() && !pinSalt.isNullOrEmpty()) {
                    target.putString(KEY_PIN_SALT, pinSalt)
                    target.putString(KEY_PIN_HASH, pinHash)
                    if (reqQ != null) target.putString(KEY_RECOVERY_QUESTION, reqQ)
                    if (recH != null) target.putString(KEY_RECOVERY_HASH, recH)
                    if (recS != null) target.putString(KEY_RECOVERY_SALT, recS)
                    legacy.edit().clear().apply()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        ctx.deleteSharedPreferences(LEGACY_PREFS_NAME)
                    }
                    Log.i("PinManager", "Migrated PIN storage from legacy EncryptedSharedPreferences")
                    return
                }
            }
        }.onFailure {
            Log.d("PinManager", "Legacy EncryptedSharedPreferences not present or inaccessible: ${it.message}")
        }

        // Attempt 2: Check fallback preferences ("pin_store_fallback")
        runCatching {
            val fallback = ctx.getSharedPreferences("${LEGACY_PREFS_NAME}_fallback", Context.MODE_PRIVATE)
            if (fallback.contains(KEY_PIN_HASH)) {
                val pinHash = fallback.getString(KEY_PIN_HASH, null)
                val pinSalt = fallback.getString(KEY_PIN_SALT, null)
                val reqQ = fallback.getString(KEY_RECOVERY_QUESTION, null)
                val recH = fallback.getString(KEY_RECOVERY_HASH, null)
                val recS = fallback.getString(KEY_RECOVERY_SALT, null)
                if (!pinHash.isNullOrEmpty() && !pinSalt.isNullOrEmpty()) {
                    target.putString(KEY_PIN_SALT, pinSalt)
                    target.putString(KEY_PIN_HASH, pinHash)
                    if (reqQ != null) target.putString(KEY_RECOVERY_QUESTION, reqQ)
                    if (recH != null) target.putString(KEY_RECOVERY_HASH, recH)
                    if (recS != null) target.putString(KEY_RECOVERY_SALT, recS)
                    fallback.edit().clear().apply()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        ctx.deleteSharedPreferences("${LEGACY_PREFS_NAME}_fallback")
                    }
                    Log.i("PinManager", "Migrated PIN storage from legacy fallback preferences")
                }
            }
        }
    }

    fun isPinSet(): Boolean =
        runCatching {
            storage.contains(KEY_PIN_HASH)
        }.getOrElse {
            keystoreCorrupted = true
            false
        }

    fun setPin(pin: String) {
        val salt = HashUtils.randomSalt()
        runCatching {
            storage.putString(KEY_PIN_SALT, HashUtils.saltToHex(salt))
            storage.putString(KEY_PIN_HASH, HashUtils.hash(pin, salt))
        }.onFailure {
            keystoreCorrupted = true
            Log.e("PinManager", "Failed to set PIN", it)
        }
    }

    fun verifyPin(pin: String): Boolean =
        runCatching {
            val saltHex = storage.getString(KEY_PIN_SALT) ?: return false
            val storedHash = storage.getString(KEY_PIN_HASH) ?: return false
            val salt = HashUtils.saltFromHex(saltHex)
            val valid = HashUtils.verify(pin, salt, storedHash)
            if (valid && HashUtils.needsUpgrade(storedHash)) {
                val newSalt = HashUtils.randomSalt()
                storage.putString(KEY_PIN_SALT, HashUtils.saltToHex(newSalt))
                storage.putString(KEY_PIN_HASH, HashUtils.hash(pin, newSalt))
            }
            valid
        }.getOrElse {
            keystoreCorrupted = true
            false
        }

    fun setRecovery(
        question: String,
        answer: String,
    ) {
        val salt = HashUtils.randomSalt()
        runCatching {
            storage.putString(KEY_RECOVERY_QUESTION, question)
            storage.putString(KEY_RECOVERY_SALT, HashUtils.saltToHex(salt))
            storage.putString(KEY_RECOVERY_HASH, HashUtils.hash(answer.trim().lowercase(), salt))
        }.onFailure {
            keystoreCorrupted = true
            Log.e("PinManager", "Failed to set recovery", it)
        }
    }

    fun getRecoveryQuestion(): String? =
        runCatching {
            storage.getString(KEY_RECOVERY_QUESTION)
        }.getOrElse {
            keystoreCorrupted = true
            null
        }

    fun hasRecovery(): Boolean =
        runCatching {
            storage.contains(KEY_RECOVERY_HASH)
        }.getOrElse {
            keystoreCorrupted = true
            false
        }

    fun isRecoveryLockedOut(): Boolean = recoveryTracker.isRecoveryLockedOut()

    fun remainingRecoveryCooldownSeconds(): Long = recoveryTracker.remainingCooldownSeconds()

    fun recoveryLockedOutUntilMillis(): Long = recoveryTracker.recoveryLockedOutUntilMillis()

    fun recoveryFailedAttempts(): Int = recoveryTracker.failedAttempts()

    fun recordRecoveryFailure(): Boolean = recoveryTracker.recordFailure()

    fun clearRecoveryFailures() = recoveryTracker.clearRecoveryFailures()

    fun verifyRecoveryAnswer(answer: String): Boolean =
        runCatching {
            if (isRecoveryLockedOut()) return false
            val saltHex = storage.getString(KEY_RECOVERY_SALT) ?: return false
            val storedHash = storage.getString(KEY_RECOVERY_HASH) ?: return false
            val salt = HashUtils.saltFromHex(saltHex)
            val normalizedAnswer = answer.trim().lowercase()
            val valid = HashUtils.verify(normalizedAnswer, salt, storedHash)
            if (valid) {
                clearRecoveryFailures()
                if (HashUtils.needsUpgrade(storedHash)) {
                    val newSalt = HashUtils.randomSalt()
                    storage.putString(KEY_RECOVERY_SALT, HashUtils.saltToHex(newSalt))
                    storage.putString(KEY_RECOVERY_HASH, HashUtils.hash(normalizedAnswer, newSalt))
                }
            } else {
                recordRecoveryFailure()
            }
            valid
        }.getOrElse {
            keystoreCorrupted = true
            false
        }

    fun resetCorruptedStorage(): Boolean {
        return runCatching {
            runCatching {
                KeystoreEncryptedStorage.deleteKey()
            }
            if (context != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.deleteSharedPreferences(KeystoreEncryptedStorage.DEFAULT_PREFS_NAME)
                    context.deleteSharedPreferences("${KeystoreEncryptedStorage.DEFAULT_PREFS_NAME}_fallback")
                    context.deleteSharedPreferences(LEGACY_PREFS_NAME)
                    context.deleteSharedPreferences("${LEGACY_PREFS_NAME}_fallback")
                } else {
                    context.getSharedPreferences(
                        KeystoreEncryptedStorage.DEFAULT_PREFS_NAME,
                        Context.MODE_PRIVATE,
                    ).edit().clear().commit()
                    context.getSharedPreferences(
                        "${KeystoreEncryptedStorage.DEFAULT_PREFS_NAME}_fallback",
                        Context.MODE_PRIVATE,
                    ).edit().clear().commit()
                }
                storage = initStorage()
            } else {
                storage.clear()
            }
            keystoreCorrupted = false
            true
        }.getOrElse {
            Log.e("PinManager", "Failed to reset corrupted storage", it)
            false
        }
    }

    companion object {
        private const val LEGACY_PREFS_NAME = "pin_store"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_RECOVERY_QUESTION = "recovery_question"
        private const val KEY_RECOVERY_HASH = "recovery_hash"
        private const val KEY_RECOVERY_SALT = "recovery_salt"
    }
}
