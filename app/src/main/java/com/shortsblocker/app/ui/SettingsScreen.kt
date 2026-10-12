package com.shortsblocker.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shortsblocker.app.R
import com.shortsblocker.app.domain.IntentLauncher
import com.shortsblocker.app.prefs.BlockingPreferences
import com.shortsblocker.app.prefs.SupportedApps

@Composable
fun SettingsScreen(blockingPreferences: BlockingPreferences, onChangePin: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)

        Button(onClick = {
            IntentLauncher.openAccessibilitySettings(context)
        }) {
            Text(stringResource(R.string.settings_open_accessibility))
        }

        Button(onClick = {
            IntentLauncher.openOverlaySettings(context)
        }) {
            Text(stringResource(R.string.settings_open_overlay))
        }

        Button(onClick = {
            IntentLauncher.openBatteryOptimizationSettings(context)
        }) {
            Text(stringResource(R.string.settings_open_battery))
        }

        Text(
            text = stringResource(R.string.onboarding_oem_hint),
            style = MaterialTheme.typography.bodySmall,
        )

        Button(onClick = {
            IntentLauncher.openOemAutostartSettings(context)
        }) {
            Text(stringResource(R.string.onboarding_oem_button))
        }

        Button(onClick = onChangePin) {
            Text(stringResource(R.string.settings_change_pin))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(stringResource(R.string.settings_per_app_title), style = MaterialTheme.typography.titleMedium)
        SupportedApps.ALL.forEach { app ->
            var enabled by remember { mutableStateOf(blockingPreferences.isPackageEnabled(app.packageName)) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(app.displayName)
                    app.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        enabled = checked
                        blockingPreferences.setPackageEnabled(app.packageName, checked)
                    },
                )
            }
        }
    }
}
