package com.shortsblocker.app.rules

import com.shortsblocker.app.data.SupportedApps
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PackageListSyncTest {
    private fun findProjectFile(relativePath: String): File {
        val candidates =
            listOf(
                File(relativePath),
                File("app", relativePath),
                File("..", relativePath),
            )
        return candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("File not found: $relativePath in candidates: $candidates")
    }

    @Test
    fun `all packages in block_rules json are declared in accessibility_service_config xml`() {
        val configFile = findProjectFile("src/main/res/xml/accessibility_service_config.xml")
        val configXml = configFile.readText()

        val packageAttrRegex = Regex("""android:packageNames="([^"]+)"""")
        val match =
            packageAttrRegex.find(configXml)
                ?: throw IllegalStateException("Could not find android:packageNames attribute in ${configFile.path}")
        val declaredPackages = match.groupValues[1].split(",").map { it.trim() }.toSet()

        val rulesFile = findProjectFile("src/main/assets/block_rules.json")
        val rulesJson = JSONObject(rulesFile.readText())

        val targetPackages = rulesJson.getJSONObject("packages").keys().asSequence().toSet()
        val browserPackages = rulesJson.optJSONObject("browsers")?.keys()?.asSequence()?.toSet() ?: emptySet()
        val allRequiredPackages = targetPackages + browserPackages

        for (pkg in allRequiredPackages) {
            assertTrue(
                "Package '$pkg' is defined in block_rules.json but missing from accessibility_service_config.xml",
                declaredPackages.contains(pkg),
            )
        }
    }

    @Test
    fun `all supported apps in SupportedApps ALL are declared in accessibility_service_config xml`() {
        val configFile = findProjectFile("src/main/res/xml/accessibility_service_config.xml")
        val configXml = configFile.readText()

        val packageAttrRegex = Regex("""android:packageNames="([^"]+)"""")
        val match =
            packageAttrRegex.find(configXml)
                ?: throw IllegalStateException("Could not find android:packageNames attribute in ${configFile.path}")
        val declaredPackages = match.groupValues[1].split(",").map { it.trim() }.toSet()

        for (app in SupportedApps.ALL) {
            assertTrue(
                "SupportedApp '${app.packageName}' is missing from accessibility_service_config.xml",
                declaredPackages.contains(app.packageName),
            )
        }
    }
}
