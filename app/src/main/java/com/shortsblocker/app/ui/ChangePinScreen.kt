package com.shortsblocker.app.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.shortsblocker.app.R
import com.shortsblocker.app.security.PinAttemptTracker
import com.shortsblocker.app.security.PinManager
import com.shortsblocker.app.security.PinValidationResult
import com.shortsblocker.app.security.PinValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChangePinScreen(
    pinManager: PinManager,
    onPinChanged: () -> Unit,
) {
    SecureFlag()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tracker = remember { PinAttemptTracker(context) }

    var step by remember { mutableStateOf(1) } // 1: verify current PIN, 2: set new PIN
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var question by remember { mutableStateOf(pinManager.getRecoveryQuestion() ?: "") }
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember { mutableStateOf(tracker.remainingLockoutSeconds()) }
    var isLoading by remember { mutableStateOf(false) }

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
                        imageVector = if (step == 1) Icons.Default.Lock else Icons.Default.LockReset,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.change_pin_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (step == 1) "Step 1 of 2: Verify current PIN" else "Step 2 of 2: Set your new PIN",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

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
                enabled = lockoutSeconds == 0L && !isLoading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            if (lockoutSeconds > 0) {
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
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = stringResource(R.string.pin_entry_locked_out, lockoutSeconds),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            } else if (error != null) {
                val remaining = (5 - tracker.failedAttempts()).coerceAtLeast(1)
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
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = stringResource(R.string.pin_entry_tries_left, remaining),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (tracker.isLockedOut()) {
                        lockoutSeconds = tracker.remainingLockoutSeconds()
                        return@Button
                    }
                    coroutineScope.launch {
                        isLoading = true
                        try {
                            val valid =
                                withContext(Dispatchers.IO) {
                                    pinManager.verifyPin(currentPin)
                                }
                            if (valid) {
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
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = lockoutSeconds == 0L && currentPin.isNotEmpty() && !isLoading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
            ) {
                if (isLoading) {
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
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                supportingText = {
                    if (newPin.isNotEmpty()) {
                        Text(stringResource(R.string.pin_setup_length_counter, newPin.length))
                    }
                },
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
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                supportingText = {
                    if (confirmNewPin.isNotEmpty() && newPin.isNotEmpty()) {
                        val matches = newPin == confirmNewPin
                        Text(
                            text =
                                if (matches) {
                                    stringResource(R.string.pin_setup_match_indicator)
                                } else {
                                    stringResource(R.string.pin_setup_mismatch_indicator)
                                },
                            color = if (matches) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = question,
                onValueChange = {
                    question = it
                    error = null
                },
                label = { Text(stringResource(R.string.pin_setup_recovery_question_label)) },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = answer,
                onValueChange = {
                    answer = it
                    error = null
                },
                label = { Text(stringResource(R.string.pin_setup_recovery_answer_label)) },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

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
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(text = it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                                coroutineScope.launch {
                                    isLoading = true
                                    try {
                                        withContext(Dispatchers.IO) {
                                            pinManager.setPin(newPin)
                                            pinManager.setRecovery(question, answer)
                                        }
                                        onPinChanged()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    }
                },
                enabled = !isLoading && newPin.isNotEmpty() && confirmNewPin.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
            ) {
                if (isLoading) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                        Text(stringResource(R.string.pin_saving))
                    }
                } else {
                    Text(stringResource(R.string.change_pin_save))
                }
            }
        }
    }
}
