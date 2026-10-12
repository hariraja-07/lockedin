package com.shortsblocker.app.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.shortsblocker.app.R
import com.shortsblocker.app.security.PinAttemptTracker
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
fun PinSetupScreen(pinManager: PinManager, onDone: () -> Unit) {
    SecureFlag()
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
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
        )
        OutlinedTextField(
            value = confirmPin,
            onValueChange = { confirmPin = it.filter(Char::isDigit); error = null },
            label = { Text(stringResource(R.string.pin_setup_confirm_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        OutlinedTextField(
            value = question,
            onValueChange = { question = it; error = null },
            label = { Text(stringResource(R.string.pin_setup_recovery_question_label)) },
        )
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it; error = null },
            label = { Text(stringResource(R.string.pin_setup_recovery_answer_label)) },
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(onClick = {
            when (PinValidator.validate(pin, confirmPin)) {
                is PinValidationResult.TooShort -> error = pinTooShortError
                is PinValidationResult.Mismatch -> error = mismatchError
                is PinValidationResult.TrivialPin -> error = trivialError
                is PinValidationResult.Valid -> {
                    if (question.isBlank() || answer.isBlank()) {
                        error = missingRecoveryError
                    } else {
                        pinManager.setPin(pin)
                        pinManager.setRecovery(question, answer)
                        onDone()
                    }
                }
            }
        }) {
            Text(stringResource(R.string.pin_setup_confirm))
        }
    }
}

@Composable
fun PinEntryScreen(pinManager: PinManager, onSuccess: () -> Unit, onForgot: () -> Unit) {
    SecureFlag()
    val context = LocalContext.current
    val tracker = remember { PinAttemptTracker(context) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var lockoutSeconds by remember { mutableStateOf(tracker.remainingLockoutSeconds()) }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000L)
            lockoutSeconds = tracker.remainingLockoutSeconds()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.pin_entry_title), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit); error = false },
            label = { Text(stringResource(R.string.pin_entry_title)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            enabled = lockoutSeconds == 0L,
        )
        if (lockoutSeconds > 0) {
            Text(
                text = "Too many incorrect attempts. Try again in ${lockoutSeconds}s",
                color = MaterialTheme.colorScheme.error,
            )
        } else if (error) {
            val remainingBeforeLockout = (5 - tracker.failedAttempts()).coerceAtLeast(1)
            Text(
                text = "Incorrect PIN ($remainingBeforeLockout attempts remaining)",
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = {
                if (tracker.isLockedOut()) {
                    lockoutSeconds = tracker.remainingLockoutSeconds()
                    return@Button
                }
                if (pinManager.verifyPin(pin)) {
                    tracker.recordSuccess()
                    onSuccess()
                } else {
                    val wait = tracker.recordFailure()
                    if (wait > 0) {
                        lockoutSeconds = wait
                    }
                    error = true
                }
            },
            enabled = lockoutSeconds == 0L && pin.isNotEmpty(),
        ) {
            Text(stringResource(R.string.pin_entry_confirm))
        }
        TextButton(onClick = onForgot) {
            Text(stringResource(R.string.pin_entry_forgot))
        }
    }
}

@Composable
fun PinRecoveryScreen(pinManager: PinManager, onRecovered: () -> Unit) {
    SecureFlag()
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val question = remember { pinManager.getRecoveryQuestion() ?: "" }
    var cooldownSeconds by remember { mutableStateOf(pinManager.remainingRecoveryCooldownSeconds()) }

    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000)
            cooldownSeconds = pinManager.remainingRecoveryCooldownSeconds()
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
            enabled = cooldownSeconds == 0L,
        )
        if (cooldownSeconds > 0) {
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
            val remaining = (PinRecoveryTracker.MAX_ATTEMPTS - pinManager.recoveryFailedAttempts()).coerceAtLeast(1)
            Text(
                text = stringResource(R.string.pin_recovery_attempts_remaining, remaining),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = {
                if (pinManager.isRecoveryLockedOut()) {
                    cooldownSeconds = pinManager.remainingRecoveryCooldownSeconds()
                    return@Button
                }
                if (pinManager.verifyRecoveryAnswer(answer)) {
                    onRecovered()
                } else {
                    error = true
                    if (pinManager.isRecoveryLockedOut()) {
                        cooldownSeconds = pinManager.remainingRecoveryCooldownSeconds()
                    }
                }
            },
            enabled = cooldownSeconds == 0L && answer.isNotBlank(),
        ) {
            Text(stringResource(R.string.pin_recovery_confirm))
        }
    }
}
