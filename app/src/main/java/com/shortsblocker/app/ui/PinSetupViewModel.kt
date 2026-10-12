package com.shortsblocker.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shortsblocker.app.security.PinManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PinSetupViewModel(
    application: Application,
    val pinManager: PinManager = PinManager(application),
) : AndroidViewModel(application) {

    fun savePin(pin: String, question: String, answer: String, onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                pinManager.setPin(pin)
                pinManager.setRecovery(question, answer)
            }
            onDone()
        }
    }
}
