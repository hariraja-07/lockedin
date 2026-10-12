package com.shortsblocker.app.data

import android.content.Context
import com.shortsblocker.app.rules.RuleSet
import com.shortsblocker.app.rules.RuleSetParser

/**
 * Loads the bundled block_rules.json. Rules ship inside the app resources
 * only (no remote fetch) — see docs/DECISIONS.md for why.
 */
class RuleRepository(private val context: Context) {
    @Volatile
    private var cached: RuleSet? = null

    fun get(): RuleSet {
        return cached ?: synchronized(this) {
            cached ?: load().also { cached = it }
        }
    }

    private fun load(): RuleSet {
        val json = context.assets.open(RULES_ASSET_NAME).bufferedReader().use { it.readText() }
        return RuleSetParser.parse(json)
    }

    companion object {
        private const val RULES_ASSET_NAME = "block_rules.json"
    }
}
