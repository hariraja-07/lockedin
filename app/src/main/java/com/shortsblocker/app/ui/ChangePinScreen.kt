package com.shortsblocker.app.ui

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
import androidx.compose.runtime.Composable
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
import com.shortsblocker.app.security.PinValidationResult
import com.shortsblocker.app.security.PinValidator
import kotlinx.coroutines.delay

@Composable
fun ChangePinScreen(
    pinManager: PinManager,
    onPinChanged: () -> Unit,
) {
    SecureFlag()
    val context = LocalContext.current
    val tracker = remember { PinAttemptTracker(context) }

    var step by remember { mutableStateOf(1) } // 1: verify current PIN, 2: set new PIN
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var question by remember { mutableStateOf(pinManager.getRecoveryQuestion() ?: "") }
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember { mutableStateOf(tracker.remainingLockoutSeconds()) }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000)
            lockoutSeconds = tracker.remainingLockoutSeconds()
        }
    }

    val currentPinError = stringResource(R.string.change_pin_current_error)
    val pinTooShortError = stringResource(R.string.pin_setup_error_pin_too_short)
    val mismatchError = stringResource(R.string.pin_setup_error_mismatch)
    val trivialError = stringResource(R.string.pin_setup_error_trivial)
    val missingRecoveryError = stringResource(R.string.pin_setup_error_missing_recovery)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.change_pin_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        if (step == 1) {
            OutlinedTextField(
                value = currentPin,
                onValueChange = {
                    currentPin = it.filter(Char::isDigit)
                    error = null
                },
                label = { Text(stringResource(R.string.change_pin_current_label)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                enabled = lockoutSeconds == 0L,
                modifier = Modifier.fillMaxWidth(),
            )
            if (lockoutSeconds > 0) {
                Text(
                    text = stringResource(R.string.pin_entry_locked_out, lockoutSeconds),
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (error != null) {
                val remaining = (5 - tracker.failedAttempts()).coerceAtLeast(1)
                Text(
                    text = stringResource(R.string.pin_entry_tries_left, remaining),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = {
                    if (tracker.isLockedOut()) {
                        lockoutSeconds = tracker.remainingLockoutSeconds()
                        return@Button
                    }
                    if (pinManager.verifyPin(currentPin)) {
                        tracker.recordSuccess()
                        error = null
                        step = 2
                    } else {
                        val wait = tracker.recordFailure()
                        if (wait > 0) {
                            lockoutSeconds = wait
                        }
                        error = currentPinError
                    }
                },
                enabled = lockoutSeconds == 0L && currentPin.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.pin_entry_confirm))
            }
        } else {
            OutlinedTextField(
                value = newPin,
                onValueChange = {
                    newPin = it.filter(Char::isDigit)
                    error = null
                },
                label = { Text(stringResource(R.string.change_pin_new_label)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = confirmNewPin,
                onValueChange = {
                    confirmNewPin = it.filter(Char::isDigit)
                    error = null
                },
                label = { Text(stringResource(R.string.change_pin_confirm_label)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = question,
                onValueChange = {
                    question = it
                    error = null
                },
                label = { Text(stringResource(R.string.pin_setup_recovery_question_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = answer,
                onValueChange = {
                    answer = it
                    error = null
                },
                label = { Text(stringResource(R.string.pin_setup_recovery_answer_label)) },
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = {
                    when (PinValidator.validate(newPin, confirmNewPin)) {
                        is PinValidationResult.TooShort -> error = pinTooShortError
                        is PinValidationResult.Mismatch -> error = mismatchError
                        is PinValidationResult.TrivialPin -> error = trivialError
                        is PinValidationResult.Valid -> {
                            if (question.isBlank() || answer.isBlank()) {
                                error = missingRecoveryError
                            } else {
                                pinManager.setPin(newPin)
                                pinManager.setRecovery(question, answer)
                                onPinChanged()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.change_pin_save))
            }
        }
    }
}
