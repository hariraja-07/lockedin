package com.shortsblocker.app.rules

/** In-memory fake node tree so RuleEvaluator can be tested without Robolectric/Android. */
class FakeAccessibleNode(
    override val viewId: String? = null,
    override val text: String? = null,
    override val contentDescription: String? = null,
    override val className: String? = null,
    override val isVisibleToUser: Boolean = true,
    private val children: List<FakeAccessibleNode> = emptyList(),
) : AccessibleNode {

    var recycled: Boolean = false
        private set

    override val childCount: Int get() = children.size

    override fun getChild(index: Int): AccessibleNode? = children.getOrNull(index)

    override fun recycle() {
        recycled = true
    }
}
