package com.shortsblocker.app.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.shortsblocker.app.R
import com.shortsblocker.app.security.PinManager

@Composable
fun PinSetupScreen(pinManager: PinManager, onDone: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.pin_setup_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.pin_setup_body))

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit) },
            label = { Text(stringResource(R.string.pin_setup_title)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text(stringResource(R.string.pin_setup_recovery_question_label)) },
        )
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it },
            label = { Text(stringResource(R.string.pin_setup_recovery_answer_label)) },
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(onClick = {
            when {
                pin.length < 4 -> error = "PIN은 4자리 이상 숫자여야 합니다"
                question.isBlank() || answer.isBlank() -> error = "복구 질문과 답변을 입력하세요"
                else -> {
                    pinManager.setPin(pin)
                    pinManager.setRecovery(question, answer)
                    onDone()
                }
            }
        }) {
            Text(stringResource(R.string.pin_setup_confirm))
        }
    }
}

@Composable
fun PinEntryScreen(pinManager: PinManager, onSuccess: () -> Unit, onForgot: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

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
        )
        if (error) {
            Text(stringResource(R.string.pin_entry_error), color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = {
            if (pinManager.verifyPin(pin)) onSuccess() else error = true
        }) {
            Text(stringResource(R.string.pin_entry_confirm))
        }
        TextButton(onClick = onForgot) {
            Text(stringResource(R.string.pin_entry_forgot))
        }
    }
}

@Composable
fun PinRecoveryScreen(pinManager: PinManager, onRecovered: () -> Unit) {
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val question = remember { pinManager.getRecoveryQuestion() ?: "" }

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
        )
        if (error) {
            Text(stringResource(R.string.pin_recovery_error), color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = {
            if (pinManager.verifyRecoveryAnswer(answer)) onRecovered() else error = true
        }) {
            Text(stringResource(R.string.pin_recovery_confirm))
        }
    }
}
