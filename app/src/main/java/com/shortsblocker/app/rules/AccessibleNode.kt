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

    /**
     * Whether the node is actually visible on screen. Super-apps like
     * Instagram keep other surfaces' fragments (DM inbox, feed, ...) attached
     * to the view tree while you are on a different tab — those hidden nodes
     * must not trigger whitelist or block rules.
     */
    val isVisibleToUser: Boolean get() = true

    fun getChild(index: Int): AccessibleNode?

    /** No-op for fake nodes; real adapter recycles the wrapped AccessibilityNodeInfo. */
    fun recycle() {}
}
