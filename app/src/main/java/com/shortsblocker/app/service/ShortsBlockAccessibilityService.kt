package com.shortsblocker.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
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

    override fun onInterrupt() = Unit
}
