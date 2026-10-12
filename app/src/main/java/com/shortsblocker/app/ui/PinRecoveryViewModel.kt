package com.shortsblocker.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shortsblocker.app.security.PinManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PinRecoveryViewModel(
    application: Application,
    val pinManager: PinManager = PinManager(application),
) : AndroidViewModel(application) {

    val recoveryQuestion: String? = pinManager.getRecoveryQuestion()

    private val _isLockedOut = MutableStateFlow(pinManager.isRecoveryLockedOut())
    val isLockedOut: StateFlow<Boolean> = _isLockedOut.asStateFlow()

    private val _remainingCooldownSeconds = MutableStateFlow(pinManager.remainingRecoveryCooldownSeconds())
    val remainingCooldownSeconds: StateFlow<Long> = _remainingCooldownSeconds.asStateFlow()

    private val _failedAttempts = MutableStateFlow(pinManager.recoveryFailedAttempts())
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    fun updateCooldownTick() {
        val remaining = pinManager.remainingRecoveryCooldownSeconds()
        _remainingCooldownSeconds.value = remaining
        _isLockedOut.value = remaining > 0
    }

    fun verifyRecoveryAnswer(answer: String, onRecovered: () -> Unit, onError: () -> Unit) {
        if (pinManager.isRecoveryLockedOut()) {
            _isLockedOut.value = true
            _remainingCooldownSeconds.value = pinManager.remainingRecoveryCooldownSeconds()
            return
        }

        viewModelScope.launch {
            val valid = withContext(Dispatchers.IO) {
                pinManager.verifyRecoveryAnswer(answer)
            }
            if (valid) {
                _failedAttempts.value = 0
                _isLockedOut.value = false
                onRecovered()
            } else {
                _failedAttempts.value = pinManager.recoveryFailedAttempts()
                _isLockedOut.value = pinManager.isRecoveryLockedOut()
                _remainingCooldownSeconds.value = pinManager.remainingRecoveryCooldownSeconds()
                onError()
            }
        }
    }
}
