package com.shortsblocker.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shortsblocker.app.domain.IntentLauncher
import com.shortsblocker.app.prefs.BlockingPreferences
import com.shortsblocker.app.security.PinManager
import com.shortsblocker.app.service.AccessibilityStatus
import com.shortsblocker.app.ui.theme.ShortsBlockerTheme

class LockoutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_SECURE,
        )

        val pinManager = PinManager(applicationContext)
        val blockingPreferences = BlockingPreferences(applicationContext)

        setContent {
            ShortsBlockerTheme {
                LockoutContent(
                    onOpenAccessibility = {
                        IntentLauncher.openAccessibilitySettings(this)
                    },
                    onEmergencyUnlock = { pin ->
                        if (pinManager.verifyPin(pin)) {
                            blockingPreferences.masterEnabled = false
                            Toast.makeText(this, "Protection paused by PIN", Toast.LENGTH_SHORT).show()
                            finish()
                            true
                        } else {
                            false
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (AccessibilityStatus.isServiceEnabled(this)) {
            finish()
        }
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, LockoutActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
    }
}

@Composable
fun LockoutContent(
    onOpenAccessibility: () -> Unit,
    onEmergencyUnlock: (String) -> Boolean,
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F12))
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "Protection is off",
                color = Color(0xFFFF5252),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "LockedIn accessibility service was disabled in system settings. Re-enable it to unlock your screen.",
                color = Color(0xFFCCCCCC),
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )

            Button(
                onClick = onOpenAccessibility,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open accessibility settings", color = Color.White)
            }

            TextButton(
                onClick = { showPinDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Emergency Unlock (PIN)", color = Color(0xFFBB86FC))
            }
        }

        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false },
                title = { Text("Enter PIN to unlock") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pinText,
                            onValueChange = { if (it.length <= 8) pinText = it },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            isError = errorMessage != null,
                        )
                        errorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val success = onEmergencyUnlock(pinText)
                        if (success) {
                            showPinDialog = false
                        } else {
                            errorMessage = "Incorrect PIN"
                        }
                    }) {
                        Text("Unlock")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPinDialog = false }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}
