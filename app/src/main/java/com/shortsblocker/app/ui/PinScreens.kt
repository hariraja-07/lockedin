package com.shortsblocker.app.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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

    val pinTooShortError = stringResource(R.string.pin_setup_error_pin_too_short)
    val mismatchError = stringResource(R.string.pin_setup_error_mismatch)
    val trivialError = stringResource(R.string.pin_setup_error_trivial)
    val missingRecoveryError = stringResource(R.string.pin_setup_error_missing_recovery)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.pin_setup_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.pin_setup_body))

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit); error = null },
            label = { Text(stringResource(R.string.pin_setup_title)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = confirmPin,
            onValueChange = { confirmPin = it.filter(Char::isDigit); error = null },
            label = { Text(stringResource(R.string.pin_setup_confirm_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = question,
            onValueChange = { question = it; error = null },
            label = { Text(stringResource(R.string.pin_setup_recovery_question_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it; error = null },
            label = { Text(stringResource(R.string.pin_setup_recovery_answer_label)) },
            modifier = Modifier.fillMaxWidth(),
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

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
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        ) {
            Text(stringResource(R.string.pin_setup_confirm))
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun PinSetupScreen(pinManager: PinManager, onDone: () -> Unit) {
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

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000L)
            viewModel.updateLockoutTick()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.pin_entry_title), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = pin,
            onValueChange = {
                pin = it.filter(Char::isDigit)
                viewModel.clearError()
            },
            label = { Text(stringResource(R.string.pin_entry_title)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            enabled = !isLockedOut,
            modifier = Modifier.fillMaxWidth(),
        )
        if (isLockedOut) {
            Text(
                text = stringResource(R.string.pin_entry_locked_out, lockoutSeconds),
                color = MaterialTheme.colorScheme.error,
            )
        } else if (errorMessage != null) {
            val remainingBeforeLockout = (5 - failedAttempts).coerceAtLeast(1)
            Text(
                text = stringResource(R.string.pin_entry_tries_left, remainingBeforeLockout),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = {
                viewModel.verifyPin(pin, onSuccess = onSuccess)
            },
            enabled = !isLockedOut && pin.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        ) {
            Text(stringResource(R.string.pin_entry_confirm))
        }
        TextButton(
            onClick = onForgot,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        ) {
            Text(stringResource(R.string.pin_entry_forgot))
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun PinEntryScreen(pinManager: PinManager, onSuccess: () -> Unit, onForgot: () -> Unit) {
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

    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000)
            viewModel.updateCooldownTick()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.pin_recovery_title), style = MaterialTheme.typography.headlineMedium)
        Text(question)
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it; error = false },
            label = { Text(stringResource(R.string.pin_recovery_answer_hint)) },
            enabled = !isLockedOut,
            modifier = Modifier.fillMaxWidth(),
        )
        if (isLockedOut) {
            val hours = cooldownSeconds / 3600
            val minutes = (cooldownSeconds % 3600) / 60
            val sec = cooldownSeconds % 60
            val timeFormatted = when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m ${sec}s"
                else -> "${sec}s"
            }
            Text(
                text = stringResource(R.string.pin_recovery_cooldown_error, timeFormatted),
                color = MaterialTheme.colorScheme.error,
            )
        } else if (error) {
            val remaining = (PinRecoveryTracker.MAX_ATTEMPTS - failedAttempts).coerceAtLeast(1)
            Text(
                text = stringResource(R.string.pin_recovery_attempts_remaining, remaining),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = {
                viewModel.verifyRecoveryAnswer(
                    answer = answer,
                    onRecovered = onRecovered,
                    onError = { error = true },
                )
            },
            enabled = !isLockedOut && answer.isNotBlank(),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        ) {
            Text(stringResource(R.string.pin_recovery_confirm))
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun PinRecoveryScreen(pinManager: PinManager, onRecovered: () -> Unit) {
    PinRecoveryScreen(onRecovered = onRecovered)
}
