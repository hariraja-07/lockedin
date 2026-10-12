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

class ShortsBlockAccessibilityService : AccessibilityService() {

    private lateinit var ruleRepository: RuleRepository
    private lateinit var evaluator: RuleEvaluator
    private lateinit var logger: BlockEventLogger
    private lateinit var executor: BlockActionExecutor
    private lateinit var blockingPreferences: BlockingPreferences

    override fun onServiceConnected() {
        super.onServiceConnected()
        ruleRepository = RuleRepository(applicationContext)
        evaluator = RuleEvaluator()
        logger = BlockEventLogger(applicationContext)
        executor = BlockActionExecutor(this, BlockOverlayController(applicationContext), logger)
        blockingPreferences = BlockingPreferences(applicationContext)
        // Mutual supervision: whenever the system (re)binds this service, make
        // sure the watchdog/guard service is alive too. The system restarts
        // this service if the process dies while the service is enabled, so
        // this also revives the watchdog after an OEM process kill.
        BlockGuardForegroundService.start(applicationContext)
        ContextCompat.registerReceiver(
            this,
            debugDumpReceiver,
            IntentFilter(ACTION_DEBUG_DUMP),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        runCatching { unregisterReceiver(debugDumpReceiver) }
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
            val ids = sortedSetOf<String>()
            collectViewIds(root, ids, 0)
            Log.i(TAG_DUMP, "pkg=${root.packageName} nodes=${ids.size}")
            ids.forEach { Log.i(TAG_DUMP, it) }
        }
    }

    private fun collectViewIds(node: AccessibilityNodeInfo, out: MutableSet<String>, depth: Int) {
        if (depth > MAX_DUMP_DEPTH || out.size > MAX_DUMP_IDS) return
        node.viewIdResourceName?.let { out.add(it) }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectViewIds(it, out, depth + 1) }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (!blockingPreferences.masterEnabled || !blockingPreferences.isPackageEnabled(packageName)) return
        evaluateCurrentScreen(packageName)
    }

    /** Runs the full evaluation once and, if blocked, triggers the action executor. */
    private fun evaluateCurrentScreen(packageName: String): BlockDecision {
        if (!blockingPreferences.masterEnabled || !blockingPreferences.isPackageEnabled(packageName)) {
            return BlockDecision.Allow(whitelistMatched = false)
        }
        val root = rootInActiveWindow ?: return BlockDecision.Unmatched
        val rootAdapter = AccessibilityNodeAdapter(root)
        val ruleSet = ruleRepository.get()

        val result = evaluateFor(packageName, rootAdapter, ruleSet)
        recycleAll(result)

        when (val decision = result.decision) {
            is BlockDecision.Block -> executor.onBlockDetected(packageName, decision.ruleId) {
                evaluateCurrentScreen(packageName) is BlockDecision.Block
            }
            is BlockDecision.Unmatched -> logger.logUnmatched(packageName)
            is BlockDecision.Allow -> Unit
        }
        return result.decision
    }

    private fun evaluateFor(packageName: String, root: AccessibilityNodeAdapter, ruleSet: RuleSet): EvaluationResult {
        return if (ruleSet.browsers.containsKey(packageName)) {
            evaluator.evaluateBrowserUrl(packageName, root, ruleSet)
        } else {
            evaluator.evaluatePackage(packageName, root, ruleSet)
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
