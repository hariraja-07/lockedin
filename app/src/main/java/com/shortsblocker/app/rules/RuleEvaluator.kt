package com.shortsblocker.app.rules

/**
 * Pure, framework-independent evaluation of a node tree against a [RuleSet].
 * Whitelist always wins over block rules, and traversal is bounded so a single
 * accessibility event can never block the main thread for long.
 */
data class EvaluationResult(val decision: BlockDecision, val visitedNodes: List<AccessibleNode>)

class RuleEvaluator(
    private val maxNodesVisited: Int = 500,
    private val maxDepth: Int = 60,
) {

    fun evaluatePackage(packageName: String, root: AccessibleNode, ruleSet: RuleSet): EvaluationResult {
        val packageRule = ruleSet.packages[packageName]
            ?: return EvaluationResult(BlockDecision.Unmatched, listOf(root))
        if (packageRule.mode == PackageMode.FULL) {
            return EvaluationResult(BlockDecision.Block("${packageName}_full_block"), listOf(root))
        }

        val nodes = collectNodes(root)

        for (whitelistRule in packageRule.whitelist) {
            if (nodes.any { it.viewId != null && whitelistRule.viewIdRegex.containsMatchIn(it.viewId!!) }) {
                return EvaluationResult(BlockDecision.Allow(whitelistMatched = true), nodes)
            }
        }

        for (rule in packageRule.block) {
            val matchingNode = nodes.firstOrNull { node -> matches(node, rule) }
            if (matchingNode != null) {
                return EvaluationResult(BlockDecision.Block(rule.id), nodes)
            }
        }

        return EvaluationResult(BlockDecision.Unmatched, nodes)
    }

    fun evaluateBrowserUrl(packageName: String, root: AccessibleNode, ruleSet: RuleSet): EvaluationResult {
        val browserRule = ruleSet.browsers[packageName]
            ?: return EvaluationResult(BlockDecision.Unmatched, listOf(root))
        val nodes = collectNodes(root)
        val urlBarNode = nodes.firstOrNull { node ->
            node.viewId != null && browserRule.urlBarViewIdRegex.containsMatchIn(node.viewId!!)
        } ?: return EvaluationResult(BlockDecision.Unmatched, nodes)

        val urlText = urlBarNode.text ?: return EvaluationResult(BlockDecision.Unmatched, nodes)
        val matchedPattern = ruleSet.browserBlockUrlPatterns.firstOrNull { it.containsMatchIn(urlText) }
        val decision = if (matchedPattern != null) {
            BlockDecision.Block("browser_url_${matchedPattern.pattern}")
        } else {
            BlockDecision.Unmatched
        }
        return EvaluationResult(decision, nodes)
    }

    private fun matches(node: AccessibleNode, rule: MatchRule): Boolean {
        val viewId = node.viewId ?: return false
        if (rule.viewIdRegex == null || !rule.viewIdRegex.containsMatchIn(viewId)) return false

        rule.requireTextRegex?.let { required ->
            val text = node.text ?: return false
            if (!required.containsMatchIn(text)) return false
        }
        rule.requireContentDescRegex?.let { required ->
            val desc = node.contentDescription ?: return false
            if (!required.containsMatchIn(desc)) return false
        }
        rule.requireSelected?.let { required ->
            if (node.isSelected != required) return false
        }
        rule.requireChecked?.let { required ->
            if (node.isChecked != required) return false
        }
        return true
    }

    /** Breadth-first, depth- and count-bounded snapshot of the node tree.
     *  Invisible subtrees are skipped entirely: super-apps keep other
     *  surfaces' fragments attached, and hidden nodes must not affect the
     *  whitelist/block decision. If a device reports *everything* as
     *  invisible (some OEM quirks), fall back to the full tree rather than
     *  going blind. */
    private fun collectNodes(root: AccessibleNode): List<AccessibleNode> {
        val visible = collectNodes(root, requireVisible = true)
        val rootWasOnlyNode = visible.size <= 1 && root.childCount > 0
        return if (rootWasOnlyNode) collectNodes(root, requireVisible = false) else visible
    }

    private fun collectNodes(root: AccessibleNode, requireVisible: Boolean): List<AccessibleNode> {
        val result = ArrayList<AccessibleNode>(minOf(maxNodesVisited, 64))
        val queue: ArrayDeque<Pair<AccessibleNode, Int>> = ArrayDeque()
        queue.add(root to 0)

        while (queue.isNotEmpty() && result.size < maxNodesVisited) {
            val (node, depth) = queue.removeFirst()
            if (requireVisible && !node.isVisibleToUser) continue
            result.add(node)
            if (depth >= maxDepth) continue

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                queue.add(child to depth + 1)
            }
        }
        return result
    }
}
