package com.shortsblocker.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.shortsblocker.app.R
import com.shortsblocker.app.overlay.LockoutOverlayController
import com.shortsblocker.app.prefs.BlockingPreferences
import com.shortsblocker.app.security.PinManager
import com.shortsblocker.app.ui.MainActivity
import com.shortsblocker.app.watchdog.WatchdogDecision
import com.shortsblocker.app.watchdog.WatchdogMonitor
import com.shortsblocker.app.watchdog.WatchdogState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Persistent notification + START_STICKY so Android is less likely to kill
 * the process the accessibility service runs in, and so BootCompletedReceiver
 * has something to restart. Also runs the watchdog: if the accessibility
 * service gets switched off in system Settings while master blocking is on,
 * [WatchdogMonitor] detects it and (from commit 4 onward) triggers the lock
 * wall. Does no content inspection itself.
 */
class BlockGuardForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var blockingPreferences: BlockingPreferences
    private lateinit var pinManager: PinManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var lockoutOverlay: LockoutOverlayController

    override fun onCreate() {
        super.onCreate()
        blockingPreferences = BlockingPreferences(applicationContext)
        pinManager = PinManager(applicationContext)
        notificationManager = getSystemService(NotificationManager::class.java)
        lockoutOverlay = LockoutOverlayController(applicationContext)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(WatchdogState.Protected))
        startWatchdog()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startWatchdog() {
        serviceScope.launch {
            WatchdogMonitor(
                currentState = {
                    WatchdogDecision.state(
                        masterEnabled = blockingPreferences.masterEnabled,
                        accessibilityServiceEnabled = AccessibilityStatus.isServiceEnabled(applicationContext),
                        isPinSet = pinManager.isPinSet(),
                    )
                },
                onStateChange = { state ->
                    notificationManager.notify(NOTIFICATION_ID, buildNotification(state))
                    if (state == WatchdogState.Lockout) lockoutOverlay.show() else lockoutOverlay.hide()
                },
                initialStartupDelayMillis = 5_000L,
            ).run()
        }
    }

    private fun buildNotification(state: WatchdogState): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val textRes = when (state) {
            WatchdogState.Protected -> R.string.guard_notification_text
            WatchdogState.PausedByUser -> R.string.guard_notification_text_paused
            WatchdogState.Lockout -> R.string.guard_notification_text_lockout
        }

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(textRes))
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.guard_notification_channel_name),
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "shorts_blocker_guard"
        private const val NOTIFICATION_ID = 1

        /** Starts the guard/watchdog service if it is not already running. */
        fun start(context: android.content.Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, BlockGuardForegroundService::class.java),
            )
        }
    }
}
