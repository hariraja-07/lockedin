package com.shortsblocker.app.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.shortsblocker.app.R
import com.shortsblocker.app.prefs.BlockingPreferences
import com.shortsblocker.app.security.PinManager

class LockoutOverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentView: LinearLayout? = null
    private var temporaryGraceUntilMillis: Long = 0L

    private val pinManager by lazy { PinManager(context) }
    private val blockingPreferences by lazy { BlockingPreferences(context) }

    private var failedAttempts = 0
    private var lockoutUntilMillis = 0L

    val isShowing: Boolean get() = currentView != null

    fun show() {
        if (SystemClock.elapsedRealtime() < temporaryGraceUntilMillis) return
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
        try {
            windowManager.addView(view, params)
            currentView = view
        } catch (e: WindowManager.BadTokenException) {
            Log.w(TAG, "BadTokenException adding lockout overlay", e)
            currentView = null
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException adding lockout overlay (missing overlay permission)", e)
            currentView = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add lockout overlay view", e)
            currentView = null
        }
    }

    fun hide() {
        val view = currentView ?: return
        currentView = null
        try {
            windowManager.removeView(view)
        } catch (e: Exception) {
            Log.w(TAG, "Error removing lockout overlay view", e)
        }
    }

    fun release() {
        mainHandler.removeCallbacksAndMessages(null)
        hide()
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

        // Open Settings button: provides a 60-second grace window so user can interact with Settings
        val button = Button(context).apply {
            text = context.getString(R.string.lockout_button)
            setOnClickListener {
                temporaryGraceUntilMillis = SystemClock.elapsedRealtime() + SETTINGS_GRACE_MS
                hide()
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            }
        }

        // Emergency PIN exit container
        val pinContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, pad, 0, 0)
        }

        val pinInput = EditText(context).apply {
            hint = "Enter PIN to unlock"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            gravity = Gravity.CENTER
            visibility = View.GONE
        }

        val emergencyBtn = Button(context).apply {
            text = "Emergency Unlock (PIN)"
            setBackgroundColor(Color.DKGRAY)
            setTextColor(Color.LTGRAY)
        }

        val submitPinBtn = Button(context).apply {
            text = "Verify & Unlock"
            visibility = View.GONE
            setOnClickListener {
                val now = SystemClock.elapsedRealtime()
                if (now < lockoutUntilMillis) {
                    val remainingSec = (lockoutUntilMillis - now) / 1000
                    Toast.makeText(context, "Too many attempts. Wait ${remainingSec}s", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val enteredPin = pinInput.text.toString()
                if (pinManager.verifyPin(enteredPin)) {
                    failedAttempts = 0
                    blockingPreferences.masterEnabled = false
                    hide()
                    Toast.makeText(context, "Protection paused by PIN", Toast.LENGTH_LONG).show()
                } else {
                    failedAttempts++
                    if (failedAttempts >= MAX_ATTEMPTS) {
                        lockoutUntilMillis = now + ATTEMPT_LOCKOUT_MS
                        Toast.makeText(context, "Incorrect PIN. Locked out for 30s", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Incorrect PIN (${MAX_ATTEMPTS - failedAttempts} tries left)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        emergencyBtn.setOnClickListener {
            emergencyBtn.visibility = View.GONE
            pinInput.visibility = View.VISIBLE
            submitPinBtn.visibility = View.VISIBLE
        }

        pinContainer.addView(emergencyBtn)
        pinContainer.addView(pinInput)
        pinContainer.addView(submitPinBtn)

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(18, 18, 18))
            setPadding(pad, pad, pad, pad)
            addView(title)
            addView(body)
            addView(button)
            addView(pinContainer)
        }
    }

    companion object {
        private const val TAG = "LockoutOverlay"
        private const val SETTINGS_GRACE_MS = 60_000L
        private const val MAX_ATTEMPTS = 5
        private const val ATTEMPT_LOCKOUT_MS = 30_000L
    }
}
