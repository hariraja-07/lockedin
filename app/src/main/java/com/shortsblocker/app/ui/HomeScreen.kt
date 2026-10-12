package com.shortsblocker.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shortsblocker.app.R
import com.shortsblocker.app.data.BlockingPreferences
import com.shortsblocker.app.domain.IntentLauncher
import com.shortsblocker.app.ui.theme.StatusLockoutContainerDark
import com.shortsblocker.app.ui.theme.StatusLockoutContainerLight
import com.shortsblocker.app.ui.theme.StatusLockoutContentDark
import com.shortsblocker.app.ui.theme.StatusLockoutContentLight
import com.shortsblocker.app.ui.theme.StatusPausedContainerDark
import com.shortsblocker.app.ui.theme.StatusPausedContainerLight
import com.shortsblocker.app.ui.theme.StatusPausedContentDark
import com.shortsblocker.app.ui.theme.StatusPausedContentLight
import com.shortsblocker.app.ui.theme.StatusProtectedContainerDark
import com.shortsblocker.app.ui.theme.StatusProtectedContainerLight
import com.shortsblocker.app.ui.theme.StatusProtectedContentDark
import com.shortsblocker.app.ui.theme.StatusProtectedContentLight
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
        val observer =
            LifecycleEventObserver { _, event ->
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
                TextButton(
                    onClick = {
                        showDisableConfirm = false
                        onRequestDisable()
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) { Text(stringResource(R.string.home_disable_confirm_yes)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDisableConfirm = false },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
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
                        modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.home_log_clear_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearLogDialog = false
                        clearPinInput = ""
                        clearPinError = false
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.home_disable_confirm_cancel))
                }
            },
        )
    }

    val isDark = isSystemInDarkTheme()

    // Determine status styling
    val (statusBg, statusFg, statusTitle, statusDesc, statusIcon) =
        when {
            !isMasterEnabled ->
                StatusStyle(
                    bg = if (isDark) StatusPausedContainerDark else StatusPausedContainerLight,
                    fg = if (isDark) StatusPausedContentDark else StatusPausedContentLight,
                    title = stringResource(R.string.home_card_status_paused),
                    desc = stringResource(R.string.home_card_desc_paused),
                    icon = Icons.Default.PauseCircle,
                )
            !isServiceEnabled ->
                StatusStyle(
                    bg = if (isDark) StatusLockoutContainerDark else StatusLockoutContainerLight,
                    fg = if (isDark) StatusLockoutContentDark else StatusLockoutContentLight,
                    title = stringResource(R.string.home_card_status_lockout),
                    desc = stringResource(R.string.home_card_desc_lockout),
                    icon = Icons.Default.Warning,
                )
            else ->
                StatusStyle(
                    bg = if (isDark) StatusProtectedContainerDark else StatusProtectedContainerLight,
                    fg = if (isDark) StatusProtectedContentDark else StatusProtectedContentLight,
                    title = stringResource(R.string.home_card_status_protected),
                    desc = stringResource(R.string.home_card_desc_protected),
                    icon = Icons.Default.CheckCircle,
                )
        }

    val hasMissingPermissions =
        !protectionStatus.isAccessibilityGranted ||
            !protectionStatus.isOverlayGranted ||
            !protectionStatus.isBatteryExempted

    val filteredEvents =
        if (showBlockedOnly) {
            events.filter { it.result != "UNMATCHED" }
        } else {
            events
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // App Header Bar with Branding & Settings Quick Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Column {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.home_header_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FilledTonalIconButton(
                onClick = onOpenSettings,
                modifier =
                    Modifier
                        .size(44.dp)
                        .semantics { contentDescription = "Settings" },
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Hero Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = statusBg, contentColor = statusFg),
            border = BorderStroke(1.dp, statusFg.copy(alpha = 0.2f)),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusFg,
                            modifier = Modifier.size(28.dp),
                        )
                        Column {
                            Text(
                                text = statusTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = statusDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = statusFg.copy(alpha = 0.85f),
                            )
                        }
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
                        colors =
                            SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = statusFg,
                            ),
                        modifier =
                            Modifier.semantics {
                                contentDescription = statusTitle
                            },
                    )
                }
            }
        }

        // Daily Analytics & Health Metrics Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Blocked Count Metric
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.home_stat_blocks_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "$todayBlockedCount",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Engine Health Metric
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint =
                                if (isServiceEnabled && isMasterEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.home_stat_status_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text =
                            if (isServiceEnabled && isMasterEnabled) {
                                stringResource(R.string.home_stat_healthy)
                            } else {
                                stringResource(R.string.home_stat_attention)
                            },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color =
                            if (isServiceEnabled && isMasterEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                    )
                }
            }
        }

        // Missing Permissions Action Banner (if needed)
        if (hasMissingPermissions) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(R.string.home_perm_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
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

        // Protection Log Section Header with Filter Chips & Clear Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_recent_events_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = "${filteredEvents.size}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            FilledTonalIconButton(
                onClick = { showClearLogDialog = true },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.home_log_clear_button),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Filter Chips Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = !showBlockedOnly,
                onClick = { showBlockedOnly = false },
                label = { Text(stringResource(R.string.home_log_filter_all)) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
            )
            FilterChip(
                selected = showBlockedOnly,
                onClick = { showBlockedOnly = true },
                label = { Text(stringResource(R.string.home_log_filter_blocked)) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
            )
        }

        // Live Log List
        if (filteredEvents.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.home_empty_log_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.home_empty_log_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredEvents.reversed()) { event ->
                    val isBlocked = event.result != "UNMATCHED"
                    val timeText =
                        remember(event.timestampMillis) {
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestampMillis))
                        }
                    val friendlyAppName =
                        when {
                            event.packageName.contains("youtube") -> "YouTube Shorts"
                            event.packageName.contains("instagram") -> "Instagram Reels"
                            event.packageName.contains("chrome") -> "Chrome"
                            else -> event.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                        }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = friendlyAppName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "$timeText • ${event.result}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color =
                                    if (isBlocked) {
                                        MaterialTheme.colorScheme.errorContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = if (isBlocked) Icons.Default.Block else Icons.Default.Check,
                                        contentDescription = null,
                                        tint =
                                            if (isBlocked) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Text(
                                        text = if (isBlocked) "Blocked" else "Passed",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color =
                                            if (isBlocked) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Info footnote
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(R.string.home_log_unmatched_explanation),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
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
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalButton(
            onClick = onFix,
            colors =
                ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            modifier = Modifier.defaultMinSize(minHeight = 40.dp),
        ) {
            Text(stringResource(R.string.home_perm_fix_button))
        }
    }
}

private data class StatusStyle(
    val bg: Color,
    val fg: Color,
    val title: String,
    val desc: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun HomeScreen(
    blockingPreferences: BlockingPreferences,
    onOpenSettings: () -> Unit,
    onRequestDisable: () -> Unit,
) {
    HomeScreen(onOpenSettings = onOpenSettings, onRequestDisable = onRequestDisable)
}
