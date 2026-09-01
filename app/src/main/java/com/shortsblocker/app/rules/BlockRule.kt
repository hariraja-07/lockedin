package com.shortsblocker.app.rules

enum class PackageMode { FULL, PARTIAL }

data class MatchRule(
    val id: String,
    val viewIdRegex: Regex? = null,
    val requireTextRegex: Regex? = null,
    val requireContentDescRegex: Regex? = null,
)

data class WhitelistRule(
    val viewIdRegex: Regex,
)

data class PackageRule(
    val packageName: String,
    val mode: PackageMode,
    val block: List<MatchRule> = emptyList(),
    val whitelist: List<WhitelistRule> = emptyList(),
)

data class BrowserRule(
    val packageName: String,
    val urlBarViewIdRegex: Regex,
)

data class RuleSet(
    val version: Int,
    val packages: Map<String, PackageRule>,
    val browsers: Map<String, BrowserRule>,
    val browserBlockUrlPatterns: List<Regex>,
)
