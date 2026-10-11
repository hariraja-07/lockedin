package com.shortsblocker.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shortsblocker.app.R

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium)
        Text(text = stringResource(R.string.onboarding_body))
        Text(text = stringResource(R.string.onboarding_sideload_notice), style = MaterialTheme.typography.bodySmall)
        Text(text = stringResource(R.string.onboarding_soft_lock_notice), style = MaterialTheme.typography.bodySmall)

        Button(onClick = {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }) {
            Text(stringResource(R.string.onboarding_accessibility_button))
        }

        Button(onClick = {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            )
            context.startActivity(intent)
        }) {
            Text(stringResource(R.string.onboarding_overlay_button))
        }

        Button(onClick = {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}"),
            )
            context.startActivity(intent)
        }) {
            Text(stringResource(R.string.onboarding_battery_button))
        }

        Text(text = stringResource(R.string.onboarding_oem_hint), style = MaterialTheme.typography.bodySmall)

        Button(onClick = onFinished) {
            Text(stringResource(R.string.onboarding_pin_button))
        }
    }
}
