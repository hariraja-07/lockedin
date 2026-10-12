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

class PinSetupViewModel
    @JvmOverloads
    constructor(
        application: Application,
        val pinManager: PinManager = PinManager(application),
    ) : AndroidViewModel(application) {
        private val _isSaving = MutableStateFlow(false)
        val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

        fun savePin(
            pin: String,
            question: String,
            answer: String,
            onDone: () -> Unit,
        ) {
            viewModelScope.launch {
                _isSaving.value = true
                try {
                    withContext(Dispatchers.IO) {
                        pinManager.setPin(pin)
                        pinManager.setRecovery(question, answer)
                    }
                    onDone()
                } finally {
                    _isSaving.value = false
                }
            }
        }
    }
