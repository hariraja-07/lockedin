package com.shortsblocker.app.rules

sealed class BlockDecision {
    data class Block(val ruleId: String) : BlockDecision()
    data class Allow(val whitelistMatched: Boolean) : BlockDecision()

    /**
     * PARTIAL-mode package where the node tree matched neither a whitelist
     * nor a block rule. Treated as pass-through, but the caller must log it
     * locally (never the screen content itself) so stale rules surface as
     * reports instead of silent false negatives.
     */
    object Unmatched : BlockDecision()
}
