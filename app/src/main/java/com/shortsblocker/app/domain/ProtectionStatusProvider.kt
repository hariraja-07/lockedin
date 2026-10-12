package com.shortsblocker.app.domain

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.shortsblocker.app.data.BlockingPreferences
import com.shortsblocker.app.security.PinManager
import com.shortsblocker.app.service.AccessibilityStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProtectionStatusProvider(private val context: Context) {
    private val blockingPreferences = BlockingPreferences(context)
    private val pinManager = PinManager(context)
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val _status = MutableStateFlow(computeStatus())
    val status: StateFlow<ProtectionStatus> = _status.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val contentObserver =
        object : ContentObserver(mainHandler) {
            override fun onChange(
                selfChange: Boolean,
                uri: Uri?,
            ) {
                super.onChange(selfChange, uri)
                refresh()
            }
        }

    private var isObserverRegistered = false

    fun startObserving() {
        if (!isObserverRegistered) {
            runCatching {
                val uri = Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                context.contentResolver.registerContentObserver(uri, false, contentObserver)
                isObserverRegistered = true
            }
        }
        refresh()
    }

    fun stopObserving() {
        if (isObserverRegistered) {
            runCatching {
                context.contentResolver.unregisterContentObserver(contentObserver)
                isObserverRegistered = false
            }
        }
    }

    fun refresh() {
        _status.value = computeStatus()
    }

    fun computeStatus(): ProtectionStatus {
        val accessibility = AccessibilityStatus.isServiceEnabled(context)
        val overlay = Settings.canDrawOverlays(context)
        val battery = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        val notification =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        val master = blockingPreferences.masterEnabled
        val pin = pinManager.isPinSet()

        return ProtectionStatus(
            isAccessibilityGranted = accessibility,
            isOverlayGranted = overlay,
            isNotificationGranted = notification,
            isBatteryExempted = battery,
            isMasterBlockingEnabled = master,
            isPinSet = pin,
        )
    }
}
