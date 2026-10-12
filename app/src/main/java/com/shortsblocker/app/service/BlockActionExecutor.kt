package com.shortsblocker.app.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import com.shortsblocker.app.data.BlockEventLogger
import com.shortsblocker.app.overlay.BlockOverlayController

/**
 * Executes the actual block: BACK, then HOME if BACK didn't leave the
 * blocked screen within [RECHECK_DELAY_MS]. Debounces repeated triggers for
 * the same screen within [DEBOUNCE_MS] so it can't loop on itself.
 */
class BlockActionExecutor(
    private val service: AccessibilityService,
    private val overlay: BlockOverlayController,
    private val logger: BlockEventLogger,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastSignature: String? = null
    private var lastTriggeredAtMillis: Long = 0L

    fun onBlockDetected(packageName: String, ruleId: String, isStillBlocked: () -> Boolean) {
        val signature = "$packageName:$ruleId"
        val now = System.currentTimeMillis()
        if (signature == lastSignature && now - lastTriggeredAtMillis < DEBOUNCE_MS) return
        lastSignature = signature
        lastTriggeredAtMillis = now

        logger.logBlock(packageName, ruleId)
        overlay.show()
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)

        mainHandler.postDelayed({
            if (isStillBlocked()) {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            }
        }, RECHECK_DELAY_MS)
    }

    fun release() {
        mainHandler.removeCallbacksAndMessages(null)
        overlay.release()
    }

    companion object {
        private const val RECHECK_DELAY_MS = 200L
        private const val DEBOUNCE_MS = 1000L
    }
}
