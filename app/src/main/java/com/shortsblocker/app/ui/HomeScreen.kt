package com.shortsblocker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shortsblocker.app.R
import com.shortsblocker.app.domain.IntentLauncher
import com.shortsblocker.app.prefs.BlockingPreferences
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onRequestDisable: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val protectionStatus by viewModel.protectionStatus.collectAsState()
    val todayBlockedCount by viewModel.todayBlockedCount.collectAsState()
    val isServiceEnabled = protectionStatus.isAccessibilityGranted
    var isMasterEnabled by remember { mutableStateOf(viewModel.blockingPreferences.masterEnabled) }
    var showDisableConfirm by remember { mutableStateOf(false) }
    var events by remember { mutableStateOf(viewModel.eventLogger.recentEvents()) }
    var showBlockedOnly by rememberSaveable { mutableStateOf(false) }

    var showClearLogDialog by remember { mutableStateOf(false) }
    var clearPinInput by remember { mutableStateOf("") }
    var clearPinError by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
                isMasterEnabled = viewModel.blockingPreferences.masterEnabled
                events = viewModel.eventLogger.recentEvents()
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

    if (showClearLogDialog) {
        AlertDialog(
            onDismissRequest = {
                showClearLogDialog = false
                clearPinInput = ""
                clearPinError = false
            },
            title = { Text(stringResource(R.string.home_log_clear_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.home_log_clear_desc))
                    OutlinedTextField(
                        value = clearPinInput,
                        onValueChange = {
                            clearPinInput = it.filter(Char::isDigit)
                            clearPinError = false
                        },
                        label = { Text(stringResource(R.string.pin_entry_title)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                    if (clearPinError) {
                        Text(stringResource(R.string.pin_entry_error), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val valid = viewModel.verifyPin(clearPinInput)
                            if (valid) {
                                viewModel.clearEvents()
                                events = viewModel.eventLogger.recentEvents()
                                showClearLogDialog = false
                                clearPinInput = ""
                                clearPinError = false
                            } else {
                                clearPinError = true
                            }
                        }
                    },
                    enabled = clearPinInput.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.home_log_clear_button))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showClearLogDialog = false
                    clearPinInput = ""
                    clearPinError = false
                }) {
                    Text(stringResource(R.string.home_disable_confirm_cancel))
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        // Live Status Card
        val (cardBg, cardFg, statusTitle, statusDesc) = when {
            !isMasterEnabled -> Quadruple(
                Color(0xFFFFF8E1),
                Color(0xFFE65100),
                stringResource(R.string.home_card_status_paused),
                stringResource(R.string.home_card_desc_paused),
            )
            !isServiceEnabled -> Quadruple(
                Color(0xFFFFEBEE),
                Color(0xFFC62828),
                stringResource(R.string.home_card_status_lockout),
                stringResource(R.string.home_card_desc_lockout),
            )
            else -> Quadruple(
                Color(0xFFE8F5E9),
                Color(0xFF1B5E20),
                stringResource(R.string.home_card_status_protected),
                stringResource(R.string.home_card_desc_protected),
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBg, contentColor = cardFg),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(statusTitle, style = MaterialTheme.typography.titleLarge)
                    Text(statusDesc, style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = isMasterEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            viewModel.setMasterEnabled(true)
                            isMasterEnabled = true
                        } else {
                            showDisableConfirm = true
                        }
                    },
                )
            }
        }

        // Today's Statistics
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_stats_today_blocks, todayBlockedCount),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }

        // Missing Permissions Fix List
        val hasMissingPermissions = !protectionStatus.isAccessibilityGranted ||
            !protectionStatus.isOverlayGranted ||
            !protectionStatus.isBatteryExempted

        if (hasMissingPermissions) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_perm_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    if (!protectionStatus.isAccessibilityGranted) {
                        PermissionFixRow(
                            title = stringResource(R.string.home_perm_accessibility_title),
                            desc = stringResource(R.string.home_perm_accessibility_desc),
                            onFix = { IntentLauncher.openAccessibilitySettings(context) },
                        )
                    }
                    if (!protectionStatus.isOverlayGranted) {
                        PermissionFixRow(
                            title = stringResource(R.string.home_perm_overlay_title),
                            desc = stringResource(R.string.home_perm_overlay_desc),
                            onFix = { IntentLauncher.openOverlaySettings(context) },
                        )
                    }
                    if (!protectionStatus.isBatteryExempted) {
                        PermissionFixRow(
                            title = stringResource(R.string.home_perm_battery_title),
                            desc = stringResource(R.string.home_perm_battery_desc),
                            onFix = { IntentLauncher.openBatteryOptimizationSettings(context) },
                        )
                    }
                }
            }
        }

        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.home_settings_button))
        }

        // Decision Log Header + Filter Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_recent_events_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = !showBlockedOnly,
                    onClick = { showBlockedOnly = false },
                    label = { Text(stringResource(R.string.home_log_filter_all)) },
                )
                FilterChip(
                    selected = showBlockedOnly,
                    onClick = { showBlockedOnly = true },
                    label = { Text(stringResource(R.string.home_log_filter_blocked)) },
                )
                TextButton(onClick = { showClearLogDialog = true }) {
                    Text(stringResource(R.string.home_log_clear_button))
                }
            }
        }

        val filteredEvents = if (showBlockedOnly) {
            events.filter { it.result != "UNMATCHED" }
        } else {
            events
        }

        if (filteredEvents.isEmpty()) {
            Text(stringResource(R.string.home_no_events))
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filteredEvents.reversed()) { event ->
                    val isBlocked = event.result != "UNMATCHED"
                    val icon = if (isBlocked) "🚫 Blocked" else "✓ Unmatched"
                    val timeText = remember(event.timestampMillis) {
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestampMillis))
                    }
                    Text(
                        text = "$timeText  ${event.packageName}  ${event.result}  $icon",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.home_log_unmatched_explanation),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PermissionFixRow(
    title: String,
    desc: String,
    onFix: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = onFix) {
            Text(stringResource(R.string.home_perm_fix_button))
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Suppress("UNUSED_PARAMETER")
@Composable
fun HomeScreen(
    blockingPreferences: BlockingPreferences,
    onOpenSettings: () -> Unit,
    onRequestDisable: () -> Unit,
) {
    HomeScreen(onOpenSettings = onOpenSettings, onRequestDisable = onRequestDisable)
}
