package com.shortsblocker.app.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.shortsblocker.app.R

/**
 * Full-screen, non-dismissible lock wall shown while the accessibility
 * service is disabled. Unlike [BlockOverlayController] this window is
 * touchable and focusable: it covers everything (including system Settings)
 * and only the "Open accessibility settings" button does anything. There is
 * no close button — the watchdog removes the window once protection is
 * restored. Requires SYSTEM_ALERT_WINDOW (requested during onboarding).
 */
class LockoutOverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var currentView: LinearLayout? = null

    val isShowing: Boolean get() = currentView != null

    fun show() {
        if (currentView != null) return
        val view = buildView()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.OPAQUE,
        ).apply {
            gravity = Gravity.CENTER
        }
        runCatching { windowManager.addView(view, params) }
            .onSuccess { currentView = view }
    }

    fun hide() {
        currentView?.let { runCatching { windowManager.removeView(it) } }
        currentView = null
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

    @SuppressLint("SetTextI18n")
    private fun buildView(): LinearLayout {
        val pad = (24 * context.resources.displayMetrics.density).toInt()

        val title = TextView(context).apply {
            text = context.getString(R.string.lockout_title)
            setTextColor(Color.WHITE)
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val body = TextView(context).apply {
            text = context.getString(R.string.lockout_body)
            setTextColor(Color.LTGRAY)
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, pad, 0, pad)
        }
        val button = Button(context).apply {
            text = context.getString(R.string.lockout_button)
            setOnClickListener {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            }
        }

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(18, 18, 18))
            setPadding(pad, pad, pad, pad)
            addView(title)
            addView(body)
            addView(button)
        }
    }
}
