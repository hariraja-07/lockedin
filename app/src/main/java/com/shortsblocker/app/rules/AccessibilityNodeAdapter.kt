package com.shortsblocker.app.rules

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

/** Wraps a real [AccessibilityNodeInfo] so [RuleEvaluator] never touches the Android SDK. */
class AccessibilityNodeAdapter(private val node: AccessibilityNodeInfo) : AccessibleNode {

    override val viewId: String? get() = node.viewIdResourceName
    override val text: String? get() = node.text?.toString()
    override val contentDescription: String? get() = node.contentDescription?.toString()
    override val className: String? get() = node.className?.toString()
    override val childCount: Int get() = node.childCount
    override val isVisibleToUser: Boolean get() = node.isVisibleToUser

    override fun getChild(index: Int): AccessibleNode? {
        val child = node.getChild(index) ?: return null
        return AccessibilityNodeAdapter(child)
    }

    override fun recycle() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            node.recycle()
        }
    }
}
