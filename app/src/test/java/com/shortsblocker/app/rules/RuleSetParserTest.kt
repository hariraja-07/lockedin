package com.shortsblocker.app.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleSetParserTest {
    private val sampleJson =
        """
        {
          "version": 3,
          "packages": {
            "com.instagram.android": {
              "mode": "PARTIAL",
              "whitelist": [ { "viewIdRegex": "^direct_.*" } ],
              "block": [
                { "id": "ig_reels_viewer", "viewIdRegex": ".*clips_viewer.*" },
                {
                  "id": "ig_reels_tab",
                  "viewIdRegex": ".*tab_bar.*",
                  "requireContentDescRegex": "(?i)reels"
                }
              ]
            },
            "com.zhiliaoapp.musically": { "mode": "FULL" }
          },
          "browsers": {
            "com.android.chrome": { "urlBarViewIdRegex": "com\\.android\\.chrome:id/url_bar" }
          },
          "browserBlockUrlPatterns": [ "youtube\\.com/shorts" ]
        }
        """.trimIndent()

    @Test
    fun `parses version`() {
        assertEquals(3, RuleSetParser.parse(sampleJson).version)
    }

    @Test
    fun `parses package modes`() {
        val ruleSet = RuleSetParser.parse(sampleJson)
        assertEquals(PackageMode.PARTIAL, ruleSet.packages.getValue("com.instagram.android").mode)
        assertEquals(PackageMode.FULL, ruleSet.packages.getValue("com.zhiliaoapp.musically").mode)
    }

    @Test
    fun `parses whitelist and block rules with optional fields`() {
        val ig = RuleSetParser.parse(sampleJson).packages.getValue("com.instagram.android")
        assertEquals(1, ig.whitelist.size)
        assertTrue(ig.whitelist[0].viewIdRegex.containsMatchIn("direct_thread_view"))

        assertEquals(2, ig.block.size)
        val tabRule = ig.block.first { it.id == "ig_reels_tab" }
        assertTrue(tabRule.requireContentDescRegex!!.containsMatchIn("Reels"))
        assertEquals(null, tabRule.requireTextRegex)
        assertEquals(null, tabRule.requireSelected)
        assertEquals(null, tabRule.requireChecked)

        val withSelectedJson = """{ "version": 1, "packages": { "test.pkg": { "mode": "PARTIAL", "block": [{ "id": "t", "requireSelected": true, "requireChecked": false }] } } }"""
        val parsed = RuleSetParser.parse(withSelectedJson).packages.getValue("test.pkg").block[0]
        assertEquals(true, parsed.requireSelected)
        assertEquals(false, parsed.requireChecked)

        val compoundJson =
            """
            {
              "version": 1,
              "packages": {
                "com.instagram.android": {
                  "mode": "PARTIAL",
                  "block": [
                    {
                      "id": "ig_reels_compound",
                      "allMatch": [
                        { "viewIdRegex": ".*clips_viewer.*" },
                        { "viewIdRegex": ".*clips_tab.*", "requireSelected": true }
                      ]
                    }
                  ]
                }
              }
            }
            """.trimIndent()
        val compoundRule = RuleSetParser.parse(compoundJson).packages.getValue("com.instagram.android").block[0]
        assertEquals("ig_reels_compound", compoundRule.id)
        assertEquals(2, compoundRule.allMatch.size)
        assertTrue(compoundRule.allMatch[0].viewIdRegex!!.containsMatchIn("clips_viewer_view_pager"))
        assertEquals(true, compoundRule.allMatch[1].requireSelected)
    }

    @Test
    fun `parses browsers and url patterns`() {
        val ruleSet = RuleSetParser.parse(sampleJson)
        assertTrue(ruleSet.browsers.getValue("com.android.chrome").urlBarViewIdRegex.containsMatchIn("com.android.chrome:id/url_bar"))
        assertTrue(ruleSet.browserBlockUrlPatterns[0].containsMatchIn("youtube.com/shorts/xyz"))
    }

    @Test
    fun `missing optional sections default to empty`() {
        val ruleSet = RuleSetParser.parse("""{ "version": 1 }""")
        assertTrue(ruleSet.packages.isEmpty())
        assertTrue(ruleSet.browsers.isEmpty())
        assertTrue(ruleSet.browserBlockUrlPatterns.isEmpty())
    }
}
