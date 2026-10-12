package com.shortsblocker.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat
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
import kotlinx.coroutines.withContext

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
    private var blockingPreferences: BlockingPreferences? = null
    private var pinManager: PinManager? = null
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private var lockoutOverlay: LockoutOverlayController? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(WatchdogState.Protected),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
        serviceScope.launch(Dispatchers.IO) {
            val prefs = BlockingPreferences(applicationContext)
            val pin = PinManager(applicationContext)
            withContext(Dispatchers.Main) {
                blockingPreferences = prefs
                pinManager = pin
                lockoutOverlay = LockoutOverlayController(applicationContext)
                startWatchdog()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        lockoutOverlay?.hide()
        super.onDestroy()
    }

    private fun startWatchdog() {
        val prefs = blockingPreferences ?: return
        val pinMgr = pinManager ?: return
        val overlay = lockoutOverlay ?: return

        serviceScope.launch {
            WatchdogMonitor(
                currentState = {
                    WatchdogDecision.state(
                        masterEnabled = prefs.masterEnabled,
                        accessibilityServiceEnabled = AccessibilityStatus.isServiceEnabled(applicationContext),
                        isPinSet = pinMgr.isPinSet(),
                    )
                },
                onStateChange = { state ->
                    notificationManager.notify(NOTIFICATION_ID, buildNotification(state))
                    if (state == WatchdogState.Lockout) overlay.show() else overlay.hide()
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
        private const val TAG = "BlockGuardService"
        private const val CHANNEL_ID = "shorts_blocker_guard"
        private const val NOTIFICATION_ID = 1

        @Throws(Exception::class)
        internal fun startDirect(context: android.content.Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, BlockGuardForegroundService::class.java),
            )
        }

        /** Starts the guard/watchdog service if it is not already running. */
        fun start(context: android.content.Context) {
            try {
                startDirect(context)
            } catch (e: IllegalStateException) {
                android.util.Log.w(TAG, "Foreground service start not allowed from background, scheduling retry via WorkManager", e)
                GuardReviveWorker.schedule(context)
            } catch (e: SecurityException) {
                android.util.Log.e(TAG, "SecurityException starting guard service", e)
                GuardReviveWorker.schedule(context)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Unexpected error starting guard service", e)
            }
        }
    }
}
