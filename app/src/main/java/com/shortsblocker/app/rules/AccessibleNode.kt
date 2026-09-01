package com.shortsblocker.app.rules

/**
 * Framework-independent view over a single accessibility node.
 * Lets [RuleEvaluator] be unit-tested with fake node trees instead of real
 * android.view.accessibility.AccessibilityNodeInfo instances.
 */
interface AccessibleNode {
    val viewId: String?
    val text: String?
    val contentDescription: String?
    val className: String?
    val childCount: Int

    fun getChild(index: Int): AccessibleNode?

    /** No-op for fake nodes; real adapter recycles the wrapped AccessibilityNodeInfo. */
    fun recycle() {}
}
