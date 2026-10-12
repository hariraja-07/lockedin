package com.shortsblocker.app.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepTreeDepthTest {
    private val evaluator = RuleEvaluator(maxNodesVisited = 500, maxDepth = 60)

    @Test
    fun `tree deeper than maxDepth does not cause StackOverflow and truncates traversal`() {
        // Construct a chain of 100 deep nodes (depth 0 to 99)
        var currentNode = FakeAccessibleNode(viewId = "deep_node_99")
        for (i in 98 downTo 0) {
            currentNode =
                FakeAccessibleNode(
                    viewId = "node_$i",
                    children = listOf(currentNode),
                )
        }

        val rule = MatchRule(id = "deep_rule", viewIdRegex = Regex(".*deep_node_99.*"))
        val pkgRule =
            PackageRule(
                packageName = "test.pkg",
                mode = PackageMode.PARTIAL,
                block = listOf(rule),
            )
        val ruleSet = RuleSet(1, mapOf("test.pkg" to pkgRule), emptyMap(), emptyList())

        val result = evaluator.evaluatePackage("test.pkg", currentNode, ruleSet)

        // Traversal should terminate at depth 60 (visited 61 nodes from depth 0..60)
        assertEquals(61, result.visitedNodes.size)
        // Since deep_node_99 is at depth 99 (past maxDepth 60), it was truncated and not matched
        assertEquals(BlockDecision.Unmatched, result.decision)
    }

    @Test
    fun `tree with more than maxNodesVisited stops traversal at bound without OOM`() {
        // Create a root with 1,000 direct children
        val children =
            (1..1000).map { i ->
                FakeAccessibleNode(viewId = "child_$i")
            }
        val root = FakeAccessibleNode(viewId = "root", children = children)

        val pkgRule =
            PackageRule(
                packageName = "test.pkg",
                mode = PackageMode.PARTIAL,
                block = listOf(MatchRule(id = "last_child", viewIdRegex = Regex(".*child_999.*"))),
            )
        val ruleSet = RuleSet(1, mapOf("test.pkg" to pkgRule), emptyMap(), emptyList())

        val result = evaluator.evaluatePackage("test.pkg", root, ruleSet)

        // Must cap at 500 nodes (1 root + 499 children)
        assertEquals(500, result.visitedNodes.size)
        assertTrue(result.visitedNodes.contains(root))
    }
}
