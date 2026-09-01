package com.shortsblocker.app.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEvaluatorTest {

    private val evaluator = RuleEvaluator()

    private val youtube = PackageRule(
        packageName = "com.google.android.youtube",
        mode = PackageMode.PARTIAL,
        whitelist = listOf(WhitelistRule(Regex(".*watch_player.*"))),
        block = listOf(
            MatchRule(id = "yt_shorts_player", viewIdRegex = Regex(".*reel_player.*")),
            MatchRule(
                id = "yt_shorts_tab",
                viewIdRegex = Regex(".*bottom_bar.*"),
                requireTextRegex = Regex("(?i)^shorts$"),
            ),
        ),
    )

    private val instagram = PackageRule(
        packageName = "com.instagram.android",
        mode = PackageMode.PARTIAL,
        whitelist = listOf(WhitelistRule(Regex("^direct_.*"))),
        block = listOf(
            MatchRule(id = "ig_reels_viewer", viewIdRegex = Regex(".*clips_viewer.*")),
        ),
    )

    private val tiktok = PackageRule(
        packageName = "com.zhiliaoapp.musically",
        mode = PackageMode.FULL,
    )

    private val facebook = PackageRule(
        packageName = "com.facebook.katana",
        mode = PackageMode.PARTIAL,
        whitelist = listOf(WhitelistRule(Regex(".*newsfeed_recycler.*"))),
        block = listOf(
            MatchRule(id = "fb_reels_viewer", viewIdRegex = Regex(".*reels_player.*")),
        ),
    )

    private val snapchat = PackageRule(
        packageName = "com.snapchat.android",
        mode = PackageMode.FULL,
    )

    private val chrome = BrowserRule(
        packageName = "com.android.chrome",
        urlBarViewIdRegex = Regex("com\\.android\\.chrome:id/url_bar"),
    )

    private val ruleSet = RuleSet(
        version = 1,
        packages = mapOf(
            youtube.packageName to youtube,
            instagram.packageName to instagram,
            tiktok.packageName to tiktok,
            facebook.packageName to facebook,
            snapchat.packageName to snapchat,
        ),
        browsers = mapOf(chrome.packageName to chrome),
        browserBlockUrlPatterns = listOf(Regex("youtube\\.com/shorts")),
    )

    @Test
    fun `youtube shorts player is blocked`() {
        val root = FakeAccessibleNode(children = listOf(FakeAccessibleNode(viewId = "com.google.android.youtube:id/reel_player_page")))
        val result = evaluator.evaluatePackage(youtube.packageName, root, ruleSet)
        assertTrue(result.decision is BlockDecision.Block)
        assertEquals("yt_shorts_player", (result.decision as BlockDecision.Block).ruleId)
    }

    @Test
    fun `youtube long form watch screen is allowed even if shorts tab exists in nav`() {
        val root = FakeAccessibleNode(
            children = listOf(
                FakeAccessibleNode(viewId = "com.google.android.youtube:id/watch_player"),
                FakeAccessibleNode(viewId = "com.google.android.youtube:id/bottom_bar_item", text = "Shorts"),
            ),
        )
        val result = evaluator.evaluatePackage(youtube.packageName, root, ruleSet)
        assertEquals(BlockDecision.Allow(whitelistMatched = true), result.decision)
    }

    @Test
    fun `youtube shorts tab requires matching text, home tab is not blocked`() {
        val root = FakeAccessibleNode(children = listOf(FakeAccessibleNode(viewId = "com.google.android.youtube:id/bottom_bar_item", text = "Home")))
        val result = evaluator.evaluatePackage(youtube.packageName, root, ruleSet)
        assertEquals(BlockDecision.Unmatched, result.decision)
    }

    @Test
    fun `youtube shorts tab with matching text is blocked`() {
        val root = FakeAccessibleNode(children = listOf(FakeAccessibleNode(viewId = "com.google.android.youtube:id/bottom_bar_item", text = "Shorts")))
        val result = evaluator.evaluatePackage(youtube.packageName, root, ruleSet)
        assertTrue(result.decision is BlockDecision.Block)
    }

    @Test
    fun `instagram DM thread is always allowed even alongside a reels node`() {
        val root = FakeAccessibleNode(
            children = listOf(
                FakeAccessibleNode(viewId = "direct_thread_view"),
                FakeAccessibleNode(viewId = "com.instagram.android:id/clips_viewer_view_pager"),
            ),
        )
        val result = evaluator.evaluatePackage(instagram.packageName, root, ruleSet)
        assertEquals(BlockDecision.Allow(whitelistMatched = true), result.decision)
    }

    @Test
    fun `instagram reels viewer alone is blocked`() {
        val root = FakeAccessibleNode(children = listOf(FakeAccessibleNode(viewId = "com.instagram.android:id/clips_viewer_view_pager")))
        val result = evaluator.evaluatePackage(instagram.packageName, root, ruleSet)
        assertTrue(result.decision is BlockDecision.Block)
        assertEquals("ig_reels_viewer", (result.decision as BlockDecision.Block).ruleId)
    }

    @Test
    fun `instagram normal feed with no matching rule is unmatched`() {
        val root = FakeAccessibleNode(children = listOf(FakeAccessibleNode(viewId = "com.instagram.android:id/feed_recycler")))
        val result = evaluator.evaluatePackage(instagram.packageName, root, ruleSet)
        assertEquals(BlockDecision.Unmatched, result.decision)
    }

    @Test
    fun `tiktok is blocked entirely regardless of tree contents`() {
        val root = FakeAccessibleNode(children = listOf(FakeAccessibleNode(viewId = "direct_thread_view")))
        val result = evaluator.evaluatePackage(tiktok.packageName, root, ruleSet)
        assertTrue(result.decision is BlockDecision.Block)
    }

    @Test
    fun `facebook reels player is blocked`() {
        val root = FakeAccessibleNode(children = listOf(FakeAccessibleNode(viewId = "com.facebook.katana:id/reels_player_view")))
        val result = evaluator.evaluatePackage(facebook.packageName, root, ruleSet)
        assertTrue(result.decision is BlockDecision.Block)
        assertEquals("fb_reels_viewer", (result.decision as BlockDecision.Block).ruleId)
    }

    @Test
    fun `facebook news feed is allowed`() {
        val root = FakeAccessibleNode(children = listOf(FakeAccessibleNode(viewId = "com.facebook.katana:id/newsfeed_recycler_view")))
        val result = evaluator.evaluatePackage(facebook.packageName, root, ruleSet)
        assertEquals(BlockDecision.Allow(whitelistMatched = true), result.decision)
    }

    @Test
    fun `snapchat is blocked entirely regardless of tree contents`() {
        val root = FakeAccessibleNode()
        val result = evaluator.evaluatePackage(snapchat.packageName, root, ruleSet)
        assertTrue(result.decision is BlockDecision.Block)
    }

    @Test
    fun `unknown package yields unmatched`() {
        val root = FakeAccessibleNode()
        val result = evaluator.evaluatePackage("com.example.unknown", root, ruleSet)
        assertEquals(BlockDecision.Unmatched, result.decision)
    }

    @Test
    fun `browser url bar matching shorts pattern is blocked`() {
        val root = FakeAccessibleNode(
            children = listOf(FakeAccessibleNode(viewId = "com.android.chrome:id/url_bar", text = "youtube.com/shorts/abc123")),
        )
        val result = evaluator.evaluateBrowserUrl(chrome.packageName, root, ruleSet)
        assertTrue(result.decision is BlockDecision.Block)
    }

    @Test
    fun `browser url bar with normal watch url is unmatched`() {
        val root = FakeAccessibleNode(
            children = listOf(FakeAccessibleNode(viewId = "com.android.chrome:id/url_bar", text = "youtube.com/watch?v=abc123")),
        )
        val result = evaluator.evaluateBrowserUrl(chrome.packageName, root, ruleSet)
        assertEquals(BlockDecision.Unmatched, result.decision)
    }

    @Test
    fun `traversal is bounded by max nodes visited`() {
        val boundedEvaluator = RuleEvaluator(maxNodesVisited = 2, maxDepth = 60)
        val root = FakeAccessibleNode(
            children = listOf(
                FakeAccessibleNode(viewId = "a"),
                FakeAccessibleNode(viewId = "b"),
                FakeAccessibleNode(viewId = "com.google.android.youtube:id/reel_player_page"),
            ),
        )
        val result = boundedEvaluator.evaluatePackage(youtube.packageName, root, ruleSet)
        assertEquals(BlockDecision.Unmatched, result.decision)
        assertTrue(result.visitedNodes.size <= 2)
    }

    @Test
    fun `evaluated nodes are all marked recyclable by caller`() {
        val child = FakeAccessibleNode(viewId = "com.instagram.android:id/clips_viewer_view_pager")
        val root = FakeAccessibleNode(children = listOf(child))
        val result = evaluator.evaluatePackage(instagram.packageName, root, ruleSet)
        result.visitedNodes.forEach { it.recycle() }
        assertTrue(root.recycled)
        assertTrue(child.recycled)
    }
}
