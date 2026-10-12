package com.shortsblocker.app.service

import android.content.Context
import android.provider.Settings
import android.text.TextUtils

object AccessibilityStatus {
    fun isServiceEnabled(context: Context): Boolean {
        val expectedComponent = "${context.packageName}/${ShortsBlockAccessibilityService::class.java.name}"
        val enabledServices =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false

        if (TextUtils.isEmpty(enabledServices)) return false
        return enabledServices.split(':').any { it.equals(expectedComponent, ignoreCase = true) }
    }
}
