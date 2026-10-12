package com.shortsblocker.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shortsblocker.app.security.PinAttemptTracker
import com.shortsblocker.app.security.PinManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PinEntryViewModel
    @JvmOverloads
    constructor(
        application: Application,
        val pinManager: PinManager = PinManager(application),
        val tracker: PinAttemptTracker = PinAttemptTracker(application),
    ) : AndroidViewModel(application) {
        private val _isLockedOut = MutableStateFlow(tracker.isLockedOut())
        val isLockedOut: StateFlow<Boolean> = _isLockedOut.asStateFlow()

        private val _remainingLockoutSeconds = MutableStateFlow(tracker.remainingLockoutSeconds())
        val remainingLockoutSeconds: StateFlow<Long> = _remainingLockoutSeconds.asStateFlow()

        private val _failedAttempts = MutableStateFlow(tracker.failedAttempts())
        val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

        private val _errorMessage = MutableStateFlow<String?>(null)
        val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

        private val _isVerifying = MutableStateFlow(false)
        val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

        fun updateLockoutTick() {
            val remaining = tracker.remainingLockoutSeconds()
            _remainingLockoutSeconds.value = remaining
            _isLockedOut.value = remaining > 0
        }

        fun clearError() {
            _errorMessage.value = null
        }

        fun verifyPin(
            pin: String,
            onSuccess: () -> Unit,
        ) {
            if (tracker.isLockedOut()) {
                _isLockedOut.value = true
                _remainingLockoutSeconds.value = tracker.remainingLockoutSeconds()
                return
            }

            viewModelScope.launch {
                _isVerifying.value = true
                try {
                    val valid =
                        withContext(Dispatchers.IO) {
                            pinManager.verifyPin(pin)
                        }
                    if (valid) {
                        tracker.recordSuccess()
                        _failedAttempts.value = 0
                        _errorMessage.value = null
                        onSuccess()
                    } else {
                        val wait = tracker.recordFailure()
                        _failedAttempts.value = tracker.failedAttempts()
                        if (wait > 0) {
                            _remainingLockoutSeconds.value = wait
                            _isLockedOut.value = true
                        }
                        _errorMessage.value = "Incorrect PIN"
                    }
                } finally {
                    _isVerifying.value = false
                }
            }
        }
    }
