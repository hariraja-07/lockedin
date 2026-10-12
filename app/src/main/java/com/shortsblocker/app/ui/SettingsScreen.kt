package com.shortsblocker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shortsblocker.app.R
import com.shortsblocker.app.data.AppThemeMode
import com.shortsblocker.app.data.BlockingPreferences
import com.shortsblocker.app.data.SupportedApps
import com.shortsblocker.app.domain.IntentLauncher

@Composable
fun SettingsScreen(
    blockingPreferences: BlockingPreferences,
    onChangePin: () -> Unit,
) {
    val context = LocalContext.current
    val currentThemeMode by blockingPreferences.themeModeFlow.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)

        Button(
            onClick = { IntentLauncher.openAccessibilitySettings(context) },
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        ) {
            Text(stringResource(R.string.settings_open_accessibility))
        }

        Button(
            onClick = { IntentLauncher.openOverlaySettings(context) },
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        ) {
            Text(stringResource(R.string.settings_open_overlay))
        }

        Button(
            onClick = { IntentLauncher.openBatteryOptimizationSettings(context) },
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        ) {
            Text(stringResource(R.string.settings_open_battery))
        }

        Text(
            text = stringResource(R.string.onboarding_oem_hint),
            style = MaterialTheme.typography.bodySmall,
        )

        Button(
            onClick = { IntentLauncher.openOemAutostartSettings(context) },
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        ) {
            Text(stringResource(R.string.onboarding_oem_button))
        }

        Button(
            onClick = onChangePin,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        ) {
            Text(stringResource(R.string.settings_change_pin))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = stringResource(R.string.settings_theme_title),
            style = MaterialTheme.typography.titleMedium,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppThemeMode.entries.forEach { mode ->
                val selected = currentThemeMode == mode
                val label =
                    when (mode) {
                        AppThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                        AppThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                        AppThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                    }
                val icon =
                    when (mode) {
                        AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                        AppThemeMode.LIGHT -> Icons.Default.LightMode
                        AppThemeMode.DARK -> Icons.Default.DarkMode
                    }

                FilterChip(
                    selected = selected,
                    onClick = { blockingPreferences.themeMode = mode },
                    label = { Text(label) },
                    leadingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    modifier =
                        Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(stringResource(R.string.settings_per_app_title), style = MaterialTheme.typography.titleMedium)
        SupportedApps.ALL.forEach { app ->
            var enabled by remember { mutableStateOf(blockingPreferences.isPackageEnabled(app.packageName)) }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(app.displayName)
                    app.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        enabled = checked
                        blockingPreferences.setPackageEnabled(app.packageName, checked)
                    },
                    modifier =
                        Modifier.semantics {
                            contentDescription = app.displayName
                        },
                )
            }
        }
    }
}
