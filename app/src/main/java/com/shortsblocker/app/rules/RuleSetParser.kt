package com.shortsblocker.app.rules

import org.json.JSONObject

/**
 * Parses the bundled block_rules.json into a [RuleSet]. Kept separate from
 * Android's AssetManager so it can be unit-tested with plain strings.
 */
object RuleSetParser {
    private fun JSONObject.optRegexOrNull(key: String): Regex? = if (has(key)) Regex(getString(key)) else null

    fun parse(json: String): RuleSet {
        val root = JSONObject(json)
        val version = root.optInt("version", 1)

        val packages = HashMap<String, PackageRule>()
        val packagesJson = root.optJSONObject("packages") ?: JSONObject()
        for (packageName in packagesJson.keys()) {
            val packageJson = packagesJson.getJSONObject(packageName)
            val mode =
                when (packageJson.optString("mode", "PARTIAL")) {
                    "FULL" -> PackageMode.FULL
                    else -> PackageMode.PARTIAL
                }

            val whitelist =
                packageJson.optJSONArray("whitelist")?.let { array ->
                    (0 until array.length()).map { i ->
                        val entry = array.getJSONObject(i)
                        WhitelistRule(viewIdRegex = Regex(entry.getString("viewIdRegex")))
                    }
                } ?: emptyList()

            val block =
                packageJson.optJSONArray("block")?.let { array ->
                    (0 until array.length()).map { i ->
                        parseMatchRule(array.getJSONObject(i))
                    }
                } ?: emptyList()

            packages[packageName] = PackageRule(packageName, mode, block, whitelist)
        }

        val browsers = HashMap<String, BrowserRule>()
        val browsersJson = root.optJSONObject("browsers") ?: JSONObject()
        for (packageName in browsersJson.keys()) {
            val entry = browsersJson.getJSONObject(packageName)
            browsers[packageName] =
                BrowserRule(
                    packageName = packageName,
                    urlBarViewIdRegex = Regex(entry.getString("urlBarViewIdRegex")),
                )
        }

        val browserBlockUrlPatterns =
            root.optJSONArray("browserBlockUrlPatterns")?.let { array ->
                (0 until array.length()).map { i -> Regex(array.getString(i)) }
            } ?: emptyList()

        return RuleSet(version, packages, browsers, browserBlockUrlPatterns)
    }

    private fun parseMatchRule(
        entry: JSONObject,
        defaultId: String = "",
    ): MatchRule {
        val id = entry.optString("id", defaultId)
        val allMatch =
            entry.optJSONArray("allMatch")?.let { array ->
                (0 until array.length()).map { i ->
                    parseMatchRule(array.getJSONObject(i), defaultId = "${id}_sub_$i")
                }
            } ?: emptyList()

        return MatchRule(
            id = id,
            viewIdRegex = entry.optRegexOrNull("viewIdRegex"),
            requireTextRegex = entry.optRegexOrNull("requireTextRegex"),
            requireContentDescRegex = entry.optRegexOrNull("requireContentDescRegex"),
            requireSelected = if (entry.has("requireSelected")) entry.getBoolean("requireSelected") else null,
            requireChecked = if (entry.has("requireChecked")) entry.getBoolean("requireChecked") else null,
            allMatch = allMatch,
        )
    }
}
