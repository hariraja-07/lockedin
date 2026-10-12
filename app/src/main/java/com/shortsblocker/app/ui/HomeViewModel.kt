package com.shortsblocker.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shortsblocker.app.data.BlockEventLogger
import com.shortsblocker.app.data.BlockingPreferences
import com.shortsblocker.app.domain.ProtectionStatus
import com.shortsblocker.app.domain.ProtectionStatusProvider
import com.shortsblocker.app.security.PinManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
    private val protectionStatusProvider: ProtectionStatusProvider = ProtectionStatusProvider(application),
    val blockingPreferences: BlockingPreferences = BlockingPreferences(application),
    val eventLogger: BlockEventLogger = BlockEventLogger(application),
) : AndroidViewModel(application) {
    val protectionStatus: StateFlow<ProtectionStatus> = protectionStatusProvider.status

    private val _todayBlockedCount = MutableStateFlow(0)
    val todayBlockedCount: StateFlow<Int> = _todayBlockedCount.asStateFlow()

    init {
        protectionStatusProvider.startObserving()
        refreshBlockCount()
    }

    fun refresh() {
        protectionStatusProvider.refresh()
        refreshBlockCount()
    }

    fun refreshBlockCount() {
        viewModelScope.launch {
            val recent = eventLogger.recentEvents()
            val count = recent.count { it.result != "UNMATCHED" }
            _todayBlockedCount.value = count
        }
    }

    val pinManager: PinManager = PinManager(application)

    fun clearEvents() {
        eventLogger.clear()
        refresh()
    }

    suspend fun verifyPin(pin: String): Boolean =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            pinManager.verifyPin(pin)
        }

    fun setMasterEnabled(enabled: Boolean) {
        blockingPreferences.masterEnabled = enabled
        protectionStatusProvider.refresh()
    }

    override fun onCleared() {
        super.onCleared()
        protectionStatusProvider.stopObserving()
    }
}
