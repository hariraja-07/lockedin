package com.shortsblocker.app.data

import android.content.Context

/**
 * User-controlled on/off state — separate from the PIN store since none of
 * this is secret. [masterEnabled] pauses the whole service; per-package
 * flags let the user stop blocking one app (e.g. Facebook) without touching
 * the rest. The UI is expected to gate turning [masterEnabled] off behind a
 * confirmation + PIN, but that's enforced by the caller, not here.
 */
class BlockingPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var masterEnabled: Boolean
        get() = prefs.getBoolean(KEY_MASTER_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_MASTER_ENABLED, value).apply()
        }

    fun isPackageEnabled(packageName: String): Boolean = !disabledPackages().contains(packageName)

    fun setPackageEnabled(
        packageName: String,
        enabled: Boolean,
    ) {
        val updated = HashSet(disabledPackages())
        if (enabled) updated.remove(packageName) else updated.add(packageName)
        prefs.edit().putStringSet(KEY_DISABLED_PACKAGES, updated).apply()
    }

    private fun disabledPackages(): Set<String> {
        if (!prefs.contains(KEY_DISABLED_PACKAGES)) return DEFAULT_DISABLED_PACKAGES
        return prefs.getStringSet(KEY_DISABLED_PACKAGES, emptySet()) ?: emptySet()
    }

    companion object {
        private const val PREFS_NAME = "blocking_prefs"
        private const val KEY_MASTER_ENABLED = "master_enabled"
        private const val KEY_DISABLED_PACKAGES = "disabled_packages"

        /**
         * First-run default: only YouTube Shorts + Instagram Reels blocking are
         * on. TikTok/Facebook/Snapchat rules ship in block_rules.json but stay
         * off until the user opts in from Settings.
         */
        private val DEFAULT_DISABLED_PACKAGES =
            setOf(
                "com.zhiliaoapp.musically",
                "com.facebook.katana",
                "com.snapchat.android",
            )
    }
}
