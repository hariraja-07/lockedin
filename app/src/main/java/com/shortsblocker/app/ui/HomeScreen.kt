package com.shortsblocker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.shortsblocker.app.R
import com.shortsblocker.app.logging.BlockEventLogger
import com.shortsblocker.app.prefs.BlockingPreferences
import com.shortsblocker.app.service.AccessibilityStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    blockingPreferences: BlockingPreferences,
    onOpenSettings: () -> Unit,
    onRequestDisable: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isServiceEnabled by remember { mutableStateOf(AccessibilityStatus.isServiceEnabled(context)) }
    var isMasterEnabled by remember { mutableStateOf(blockingPreferences.masterEnabled) }
    var showDisableConfirm by remember { mutableStateOf(false) }
    val logger = remember { BlockEventLogger(context) }
    var events by remember { mutableStateOf(logger.recentEvents()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceEnabled = AccessibilityStatus.isServiceEnabled(context)
                isMasterEnabled = blockingPreferences.masterEnabled
                events = logger.recentEvents()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showDisableConfirm) {
        AlertDialog(
            onDismissRequest = { showDisableConfirm = false },
            title = { Text(stringResource(R.string.home_disable_confirm_title)) },
            text = { Text(stringResource(R.string.home_disable_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDisableConfirm = false
                    onRequestDisable()
                }) { Text(stringResource(R.string.home_disable_confirm_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showDisableConfirm = false }) {
                    Text(stringResource(R.string.home_disable_confirm_cancel))
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(
                    if (isServiceEnabled) R.string.home_status_active else R.string.home_status_inactive
                ),
            )
            Switch(
                checked = isMasterEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        blockingPreferences.masterEnabled = true
                        isMasterEnabled = true
                    } else {
                        showDisableConfirm = true
                    }
                },
                modifier = Modifier.scale(0.75f),
            )
        }

        Button(onClick = onOpenSettings) {
            Text(stringResource(R.string.home_settings_button))
        }

        Text(text = stringResource(R.string.home_recent_events_title), style = MaterialTheme.typography.titleMedium)
        if (events.isEmpty()) {
            Text(stringResource(R.string.home_no_events))
        } else {
            LazyColumn {
                items(events.reversed()) { event ->
                    val timeText = remember(event.timestampMillis) {
                        SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(event.timestampMillis))
                    }
                    Text("$timeText  ${event.packageName}  →  ${event.result}")
                }
            }
        }
    }
}
