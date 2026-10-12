package com.shortsblocker.app.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shortsblocker.app.R
import com.shortsblocker.app.domain.IntentLauncher

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val protectionStatus by viewModel.protectionStatus.collectAsState()

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val totalSteps = 4

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Top: Step indicators
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.onboarding_step_counter, currentStep + 1, totalSteps),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (i in 0 until totalSteps) {
                    val active = i == currentStep
                    val passed = i < currentStep
                    val color = when {
                        active -> MaterialTheme.colorScheme.primary
                        passed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .size(if (active) 12.dp else 8.dp)
                            .background(color, CircleShape),
                    )
                }
            }
        }

        // Center Content: Wizard Step
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (currentStep) {
                0 -> {
                    // Step 1: Welcome
                    Text(
                        text = stringResource(R.string.onboarding_step_1_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(R.string.onboarding_step_1_body),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_soft_lock_notice),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.onboarding_sideload_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                1 -> {
                    // Step 2: Accessibility Service (Required)
                    Text(
                        text = stringResource(R.string.onboarding_step_2_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(R.string.onboarding_step_2_desc),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (protectionStatus.isAccessibilityGranted) {
                        OutlinedButton(
                            onClick = { IntentLauncher.openAccessibilitySettings(context) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.onboarding_accessibility_granted), color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Button(
                            onClick = { IntentLauncher.openAccessibilitySettings(context) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.onboarding_accessibility_button))
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !protectionStatus.isAccessibilityGranted) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_restricted_settings_notice),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
                2 -> {
                    // Step 3: Overlay (Required)
                    Text(
                        text = stringResource(R.string.onboarding_step_3_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(R.string.onboarding_step_3_desc),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (protectionStatus.isOverlayGranted) {
                        OutlinedButton(
                            onClick = { IntentLauncher.openOverlaySettings(context) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.onboarding_overlay_granted), color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Button(
                            onClick = { IntentLauncher.openOverlaySettings(context) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.onboarding_overlay_button))
                        }
                    }
                }
                3 -> {
                    // Step 4: Battery Optimization & OEM Autostart (Optional)
                    Text(
                        text = stringResource(R.string.onboarding_step_4_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(R.string.onboarding_step_4_desc),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (protectionStatus.isBatteryExempted) {
                        OutlinedButton(
                            onClick = { IntentLauncher.openBatteryOptimizationSettings(context) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.onboarding_battery_granted), color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Button(
                            onClick = { IntentLauncher.openBatteryOptimizationSettings(context) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.onboarding_battery_button))
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.onboarding_oem_hint),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            OutlinedButton(
                                onClick = { IntentLauncher.openOemAutostartSettings(context) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.onboarding_oem_button))
                            }
                        }
                    }
                }
            }
        }

        // Bottom Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (currentStep > 0) {
                TextButton(onClick = { currentStep-- }) {
                    Text(stringResource(R.string.onboarding_step_back))
                }
            } else {
                Spacer(modifier = Modifier.size(1.dp))
            }

            when (currentStep) {
                0 -> {
                    Button(onClick = { currentStep++ }) {
                        Text(stringResource(R.string.onboarding_step_next))
                    }
                }
                1 -> {
                    Button(
                        onClick = { currentStep++ },
                        enabled = protectionStatus.isAccessibilityGranted,
                    ) {
                        Text(stringResource(R.string.onboarding_step_next))
                    }
                }
                2 -> {
                    Button(
                        onClick = { currentStep++ },
                        enabled = protectionStatus.isOverlayGranted,
                    ) {
                        Text(stringResource(R.string.onboarding_step_next))
                    }
                }
                3 -> {
                    Button(onClick = onFinished) {
                        Text(
                            text = stringResource(R.string.onboarding_pin_button),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
