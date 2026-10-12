package com.shortsblocker.app.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat
import com.shortsblocker.app.logging.BlockEventLogger
import com.shortsblocker.app.overlay.BlockOverlayController
import com.shortsblocker.app.prefs.BlockingPreferences
import com.shortsblocker.app.rules.AccessibilityNodeAdapter
import com.shortsblocker.app.rules.BlockDecision
import com.shortsblocker.app.rules.EvaluationResult
import com.shortsblocker.app.rules.RuleEvaluator
import com.shortsblocker.app.rules.RuleRepository
import com.shortsblocker.app.rules.RuleSet

import com.shortsblocker.app.BuildConfig

class ShortsBlockAccessibilityService : AccessibilityService() {

    private var ruleRepository: RuleRepository? = null
    private var evaluator: RuleEvaluator? = null
    private var logger: BlockEventLogger? = null
    private var executor: BlockActionExecutor? = null
    private var blockingPreferences: BlockingPreferences? = null
    private var isDebugReceiverRegistered = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        ruleRepository = RuleRepository(applicationContext)
        evaluator = RuleEvaluator()
        logger = BlockEventLogger(applicationContext)
        executor = BlockActionExecutor(this, BlockOverlayController(applicationContext), logger!!)
        blockingPreferences = BlockingPreferences(applicationContext)
        // Mutual supervision: whenever the system (re)binds this service, make
        // sure the watchdog/guard service is alive too. The system restarts
        // this service if the process dies while the service is enabled, so
        // this also revives the watchdog after an OEM process kill.
        runCatching {
            BlockGuardForegroundService.start(applicationContext)
        }.onFailure { e ->
            Log.e("ShortsBlockService", "Failed to start guard service in onServiceConnected", e)
        }
        if (BuildConfig.DEBUG && !isDebugReceiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                debugDumpReceiver,
                IntentFilter(ACTION_DEBUG_DUMP),
                ContextCompat.RECEIVER_EXPORTED,
            )
            isDebugReceiverRegistered = true
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        executor?.release()
        if (isDebugReceiverRegistered) {
            runCatching { unregisterReceiver(debugDumpReceiver) }
            isDebugReceiverRegistered = false
        }
        super.onDestroy()
    }

    /**
     * Debug diagnostics: `adb shell am broadcast -a com.shortsblocker.app.DEBUG_DUMP`
     * dumps the deduplicated viewIds (structure only, never text/content) of the
     * current window to logcat under the LockedInDump tag. Used to update
     * block_rules.json when a target app changes its view IDs.
     */
    private val debugDumpReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i(TAG_DUMP, "dump requested")
            val root = rootInActiveWindow
            if (root == null) {
                Log.i(TAG_DUMP, "root is null — no accessible window right now")
                return
            }
            try {
                val ids = sortedSetOf<String>()
                collectViewIds(root, ids, 0)
                Log.i(TAG_DUMP, "pkg=${root.packageName} nodes=${ids.size}")
                ids.forEach { Log.i(TAG_DUMP, it) }
            } finally {
                root.recycle()
            }
        }
    }

    private fun collectViewIds(node: AccessibilityNodeInfo, out: MutableSet<String>, depth: Int) {
        if (depth > MAX_DUMP_DEPTH || out.size > MAX_DUMP_IDS) return
        node.viewIdResourceName?.let { id ->
            val v = if (node.isVisibleToUser) "V" else "H"
            val s = if (node.isSelected) "S" else "-"
            val desc = node.contentDescription?.let { " [desc=$it]" } ?: ""
            out.add("$v:$s:$id$desc")
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                collectViewIds(child, out, depth + 1)
            } finally {
                child.recycle()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            val prefs = blockingPreferences ?: return
            if (ruleRepository == null || evaluator == null || logger == null || executor == null) return

            val packageName = event?.packageName?.toString() ?: return
            if (!prefs.masterEnabled || !prefs.isPackageEnabled(packageName)) return
            evaluateCurrentScreen(packageName)
        } catch (e: Exception) {
            Log.e("ShortsBlockService", "Error handling onAccessibilityEvent", e)
        }
    }

    /** Runs the full evaluation once and, if blocked, triggers the action executor. */
    private fun evaluateCurrentScreen(packageName: String): BlockDecision {
        return try {
            val prefs = blockingPreferences ?: return BlockDecision.Allow(whitelistMatched = false)
            val repo = ruleRepository ?: return BlockDecision.Unmatched
            val log = logger ?: return BlockDecision.Unmatched
            val exec = executor ?: return BlockDecision.Unmatched

            if (!prefs.masterEnabled || !prefs.isPackageEnabled(packageName)) {
                return BlockDecision.Allow(whitelistMatched = false)
            }
            val root = rootInActiveWindow ?: return BlockDecision.Unmatched
            val rootAdapter = AccessibilityNodeAdapter(root)
            val ruleSet = repo.get()

            val result = evaluateFor(packageName, rootAdapter, ruleSet)
            try {
                when (val decision = result.decision) {
                    is BlockDecision.Block -> exec.onBlockDetected(packageName, decision.ruleId) {
                        evaluateCurrentScreen(packageName) is BlockDecision.Block
                    }
                    is BlockDecision.Unmatched -> log.logUnmatched(packageName)
                    is BlockDecision.Allow -> Unit
                }
                result.decision
            } finally {
                recycleAll(result)
            }
        } catch (e: Exception) {
            Log.e("ShortsBlockService", "Error evaluating current screen for $packageName", e)
            BlockDecision.Unmatched
        }
    }

    private fun evaluateFor(packageName: String, root: AccessibilityNodeAdapter, ruleSet: RuleSet): EvaluationResult {
        val ev = evaluator ?: return EvaluationResult(BlockDecision.Unmatched, listOf(root))
        return if (ruleSet.browsers.containsKey(packageName)) {
            ev.evaluateBrowserUrl(packageName, root, ruleSet)
        } else {
            ev.evaluatePackage(packageName, root, ruleSet)
        }
    }

    private fun recycleAll(result: EvaluationResult) {
        result.visitedNodes.forEach { it.recycle() }
    }

    companion object {
        const val ACTION_DEBUG_DUMP = "com.shortsblocker.app.DEBUG_DUMP"
        private const val TAG_DUMP = "LockedInDump"
        private const val MAX_DUMP_DEPTH = 40
        private const val MAX_DUMP_IDS = 300
    }
}
