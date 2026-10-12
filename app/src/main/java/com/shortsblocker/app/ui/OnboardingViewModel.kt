package com.shortsblocker.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shortsblocker.app.domain.ProtectionStatus
import com.shortsblocker.app.domain.ProtectionStatusProvider
import kotlinx.coroutines.flow.StateFlow

class OnboardingViewModel(
    application: Application,
    private val protectionStatusProvider: ProtectionStatusProvider = ProtectionStatusProvider(application),
) : AndroidViewModel(application) {

    val protectionStatus: StateFlow<ProtectionStatus> = protectionStatusProvider.status

    init {
        protectionStatusProvider.startObserving()
    }

    fun refresh() {
        protectionStatusProvider.refresh()
    }

    override fun onCleared() {
        super.onCleared()
        protectionStatusProvider.stopObserving()
    }
}
