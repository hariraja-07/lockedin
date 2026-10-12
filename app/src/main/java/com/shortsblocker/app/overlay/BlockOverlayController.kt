package com.shortsblocker.app.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import com.shortsblocker.app.R

/** Shows a brief "blocked" toast-like overlay. Never captures or shows screen content. */
class BlockOverlayController(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentView: TextView? = null

    fun show(durationMillis: Long = 1500L) {
        mainHandler.post {
            hideInternal()

            val view =
                TextView(context).apply {
                    text = context.getString(R.string.overlay_message)
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.argb(220, 20, 20, 20))
                    setPadding(48, 32, 48, 32)
                    textSize = 16f
                }

            val overlayType =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                }

            val params =
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    overlayType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = 120
                }

            try {
                windowManager.addView(view, params)
                currentView = view
                mainHandler.postDelayed({ hideInternal() }, durationMillis)
            } catch (e: WindowManager.BadTokenException) {
                Log.w(TAG, "BadTokenException adding block overlay", e)
                currentView = null
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException adding block overlay (missing overlay permission)", e)
                currentView = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add block overlay view", e)
                currentView = null
            }
        }
    }

    fun release() {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post { hideInternal() }
    }

    private fun hideInternal() {
        val view = currentView ?: return
        currentView = null
        try {
            windowManager.removeView(view)
        } catch (e: Exception) {
            Log.w(TAG, "Error removing block overlay view", e)
        }
    }

    companion object {
        private const val TAG = "BlockOverlay"
    }
}
