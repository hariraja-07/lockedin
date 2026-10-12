package com.shortsblocker.app.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shortsblocker.app.R
import com.shortsblocker.app.security.PinManager
import com.shortsblocker.app.security.PinRecoveryTracker
import com.shortsblocker.app.security.PinValidationResult
import com.shortsblocker.app.security.PinValidator
import kotlinx.coroutines.delay

/**
 * Blocks screenshots and recents-previews of whatever screen it is called
 * from (the window's FLAG_SECURE is cleared when the composable leaves
 * composition, so only PIN/lock screens are protected, not the whole app).
 */
@Composable
fun SecureFlag() {
    val activity = LocalContext.current as? Activity ?: return
    DisposableEffect(activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

@Composable
fun PinSetupScreen(
    onDone: () -> Unit,
    viewModel: PinSetupViewModel = viewModel(),
) {
    SecureFlag()
    var pin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var question by rememberSaveable { mutableStateOf("") }
    var answer by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val isSaving by viewModel.isSaving.collectAsState()

    val pinTooShortError = stringResource(R.string.pin_setup_error_pin_too_short)
    val mismatchError = stringResource(R.string.pin_setup_error_mismatch)
    val trivialError = stringResource(R.string.pin_setup_error_trivial)
    val missingRecoveryError = stringResource(R.string.pin_setup_error_missing_recovery)

    val scrollState = rememberScrollState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Visual Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.pin_setup_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.pin_setup_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // PIN input field
        OutlinedTextField(
            value = pin,
            onValueChange = {
                pin = it.filter(Char::isDigit)
                error = null
            },
            label = { Text(stringResource(R.string.pin_setup_title)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            enabled = !isSaving,
            shape = RoundedCornerShape(12.dp),
            supportingText = {
                if (pin.isNotEmpty()) {
                    Text(stringResource(R.string.pin_setup_length_counter, pin.length))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        // Confirm PIN input field
        OutlinedTextField(
            value = confirmPin,
            onValueChange = {
                confirmPin = it.filter(Char::isDigit)
                error = null
            },
            label = { Text(stringResource(R.string.pin_setup_confirm_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            enabled = !isSaving,
            shape = RoundedCornerShape(12.dp),
            supportingText = {
                if (confirmPin.isNotEmpty() && pin.isNotEmpty()) {
                    val matches = pin == confirmPin
                    Text(
                        text =
                            if (matches) {
                                stringResource(R.string.pin_setup_match_indicator)
                            } else {
                                stringResource(R.string.pin_setup_mismatch_indicator)
                            },
                        color =
                            if (matches) {
                                Color(0xFF10B981)
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        // Recovery Question
        OutlinedTextField(
            value = question,
            onValueChange = {
                question = it
                error = null
            },
            label = { Text(stringResource(R.string.pin_setup_recovery_question_label)) },
            enabled = !isSaving,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        // Recovery Answer
        OutlinedTextField(
            value = answer,
            onValueChange = {
                answer = it
                error = null
            },
            label = { Text(stringResource(R.string.pin_setup_recovery_answer_label)) },
            enabled = !isSaving,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        // Error message card
        error?.let {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Save Button with Progress Indicator Feedback
        Button(
            onClick = {
                when (PinValidator.validate(pin, confirmPin)) {
                    is PinValidationResult.TooShort -> error = pinTooShortError
                    is PinValidationResult.Mismatch -> error = mismatchError
                    is PinValidationResult.TrivialPin -> error = trivialError
                    is PinValidationResult.Valid -> {
                        if (question.isBlank() || answer.isBlank()) {
                            error = missingRecoveryError
                        } else {
                            viewModel.savePin(pin, question, answer, onDone)
                        }
                    }
                }
            },
            enabled = !isSaving && pin.isNotEmpty() && confirmPin.isNotEmpty(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
        ) {
            if (isSaving) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = stringResource(R.string.pin_saving),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.pin_setup_confirm),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun PinSetupScreen(
    pinManager: PinManager,
    onDone: () -> Unit,
) {
    PinSetupScreen(onDone = onDone)
}

@Composable
fun PinEntryScreen(
    onSuccess: () -> Unit,
    onForgot: () -> Unit,
    viewModel: PinEntryViewModel = viewModel(),
) {
    SecureFlag()
    var pin by rememberSaveable { mutableStateOf("") }
    val isLockedOut by viewModel.isLockedOut.collectAsState()
    val lockoutSeconds by viewModel.remainingLockoutSeconds.collectAsState()
    val failedAttempts by viewModel.failedAttempts.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isVerifying by viewModel.isVerifying.collectAsState()

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000L)
            viewModel.updateLockoutTick()
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Visual Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.pin_entry_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Enter your PIN to manage protection settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // PIN Input
        OutlinedTextField(
            value = pin,
            onValueChange = {
                pin = it.filter(Char::isDigit)
                viewModel.clearError()
            },
            label = { Text(stringResource(R.string.pin_entry_title)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            enabled = !isLockedOut && !isVerifying,
            shape = RoundedCornerShape(12.dp),
            supportingText = {
                if (pin.isNotEmpty()) {
                    Text(stringResource(R.string.pin_setup_length_counter, pin.length))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        // Lockout Banner
        if (isLockedOut) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.pin_entry_locked_out, lockoutSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        } else if (errorMessage != null) {
            val remainingBeforeLockout = (5 - failedAttempts).coerceAtLeast(1)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.pin_entry_tries_left, remainingBeforeLockout),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Confirm Button with Progress Indicator
        Button(
            onClick = {
                viewModel.verifyPin(pin, onSuccess = onSuccess)
            },
            enabled = !isLockedOut && !isVerifying && pin.isNotEmpty(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
        ) {
            if (isVerifying) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Text(stringResource(R.string.pin_verifying))
                }
            } else {
                Text(stringResource(R.string.pin_entry_confirm))
            }
        }

        TextButton(
            onClick = onForgot,
            enabled = !isVerifying,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        ) {
            Text(stringResource(R.string.pin_entry_forgot))
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun PinEntryScreen(
    pinManager: PinManager,
    onSuccess: () -> Unit,
    onForgot: () -> Unit,
) {
    PinEntryScreen(onSuccess = onSuccess, onForgot = onForgot)
}

@Composable
fun PinRecoveryScreen(
    onRecovered: () -> Unit,
    viewModel: PinRecoveryViewModel = viewModel(),
) {
    SecureFlag()
    var answer by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val question = viewModel.recoveryQuestion ?: ""
    val isLockedOut by viewModel.isLockedOut.collectAsState()
    val cooldownSeconds by viewModel.remainingCooldownSeconds.collectAsState()
    val failedAttempts by viewModel.failedAttempts.collectAsState()
    val isVerifying by viewModel.isVerifying.collectAsState()

    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000)
            viewModel.updateCooldownTick()
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Visual Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.pin_recovery_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Answer your security question to reset PIN",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Question Surface Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Column {
                    Text(
                        text = "Security Question",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = question,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Answer field
        OutlinedTextField(
            value = answer,
            onValueChange = {
                answer = it
                error = false
            },
            label = { Text(stringResource(R.string.pin_recovery_answer_hint)) },
            enabled = !isLockedOut && !isVerifying,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        if (isLockedOut) {
            val hours = cooldownSeconds / 3600
            val minutes = (cooldownSeconds % 3600) / 60
            val sec = cooldownSeconds % 60
            val timeFormatted =
                when {
                    hours > 0 -> "${hours}h ${minutes}m"
                    minutes > 0 -> "${minutes}m ${sec}s"
                    else -> "${sec}s"
                }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(R.string.pin_recovery_cooldown_error, timeFormatted),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        } else if (error) {
            val remaining = (PinRecoveryTracker.MAX_ATTEMPTS - failedAttempts).coerceAtLeast(1)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(R.string.pin_recovery_attempts_remaining, remaining),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Confirm Button with Progress Indicator
        Button(
            onClick = {
                viewModel.verifyRecoveryAnswer(
                    answer = answer,
                    onRecovered = onRecovered,
                    onError = { error = true },
                )
            },
            enabled = !isLockedOut && !isVerifying && answer.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
        ) {
            if (isVerifying) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Text(stringResource(R.string.pin_verifying))
                }
            } else {
                Text(stringResource(R.string.pin_recovery_confirm))
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun PinRecoveryScreen(
    pinManager: PinManager,
    onRecovered: () -> Unit,
) {
    PinRecoveryScreen(onRecovered = onRecovered)
}
