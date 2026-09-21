package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.KeyItem
import com.example.data.model.SessionLog
import com.example.data.repository.AuthResult
import com.example.data.repository.KeyRepository
import com.example.util.DeviceUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    ACCESS,      // Login / Workspace
    MANAGEMENT,  // Admin Key List & Generator
    LOGS,        // Audit & Session History
    JSON_SYNC    // Raw JSON Schema Import & Export
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val isFirstActivation: Boolean = false
)

class KeyAuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = KeyRepository(AppDatabase.getInstance(application))

    val allKeys: StateFlow<List<KeyItem>> = repository.allKeysFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allLogs: StateFlow<List<SessionLog>> = repository.allLogsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val cloudSyncStatus = repository.cloudSyncStatus

    private val _currentTab = MutableStateFlow(AppTab.MANAGEMENT)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _inputKey = MutableStateFlow("KEY-ABC123XYZ")
    val inputKey: StateFlow<String> = _inputKey.asStateFlow()

    private val _currentDeviceId = MutableStateFlow(DeviceUtils.getCurrentDeviceId(application))
    val currentDeviceId: StateFlow<String> = _currentDeviceId.asStateFlow()

    private val _simulatedDeviceId = MutableStateFlow(DeviceUtils.getSimulatedDeviceId(application))
    val simulatedDeviceId: StateFlow<String?> = _simulatedDeviceId.asStateFlow()

    private val _activeKey = MutableStateFlow<KeyItem?>(null)
    val activeKey: StateFlow<KeyItem?> = _activeKey.asStateFlow()

    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _isBengali = MutableStateFlow(false)
    val isBengali: StateFlow<Boolean> = _isBengali.asStateFlow()

    // Global App Kill Switch / Maintenance Mode
    private val _isServerOnline = MutableStateFlow(true)
    val isServerOnline: StateFlow<Boolean> = _isServerOnline.asStateFlow()

    private val _maintenanceNotice = MutableStateFlow("")
    val maintenanceNotice: StateFlow<String> = _maintenanceNotice.asStateFlow()

    private val _isStatusUpdating = MutableStateFlow(false)
    val isStatusUpdating: StateFlow<Boolean> = _isStatusUpdating.asStateFlow()

    // 1-second ticker for live countdown in UI
    private val _ticker = MutableStateFlow(System.currentTimeMillis())
    val ticker: StateFlow<Long> = _ticker.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDefaultKeysIfNeeded()
            loadAppStatus()
        }

        // Live countdown clock
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _ticker.value = System.currentTimeMillis()

                // Check active key expiration in real-time
                val current = _activeKey.value
                if (current != null && current.expiresAt != null) {
                    if (System.currentTimeMillis() > current.expiresAt) {
                        _authUiState.value = AuthUiState(
                            errorMessage = if (_isBengali.value) "চাবির মেয়াদ শেষ হয়েছে!" else "License has expired!"
                        )
                        _activeKey.value = null
                    }
                }
            }
        }
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun onInputKeyChange(newKey: String) {
        _inputKey.value = newKey.uppercase()
        // Clear previous error when typing
        if (_authUiState.value.errorMessage != null) {
            _authUiState.value = _authUiState.value.copy(errorMessage = null)
        }
    }

    fun toggleLanguage() {
        _isBengali.value = !_isBengali.value
    }

    fun selectKeyForLogin(key: String) {
        _inputKey.value = key
        _currentTab.value = AppTab.ACCESS
    }

    fun authenticate() {
        if (!_isServerOnline.value) {
            val msg = if (_maintenanceNotice.value.isNotBlank()) {
                _maintenanceNotice.value
            } else if (_isBengali.value) {
                "সার্ভার রক্ষণাবেক্ষণের জন্য সাময়িকভাবে বন্ধ আছে।"
            } else {
                "Server is offline for maintenance. Access is temporarily disabled."
            }
            _authUiState.value = AuthUiState(
                isLoading = false,
                errorMessage = msg
            )
            return
        }

        val key = _inputKey.value.trim()
        val device = _currentDeviceId.value

        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            val result = repository.authenticate(key, device)
            when (result) {
                is AuthResult.Success -> {
                    _activeKey.value = result.keyItem
                    _authUiState.value = AuthUiState(
                        isLoading = false,
                        successMessage = result.message,
                        isFirstActivation = result.isFirstActivation
                    )
                }
                is AuthResult.Error -> {
                    _authUiState.value = AuthUiState(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun refreshActiveKey() {
        val current = _activeKey.value ?: return
        viewModelScope.launch {
            val updated = repository.getKey(current.key)
            if (updated == null || updated.isBlocked || updated.isExpired) {
                _activeKey.value = null
                _authUiState.value = AuthUiState(
                    errorMessage = if (_isBengali.value) "লাইসেন্স আর বৈধ নয়" else "License is no longer valid"
                )
            } else {
                _activeKey.value = updated
            }
        }
    }

    fun logout(unbindThisDevice: Boolean) {
        val current = _activeKey.value
        val device = _currentDeviceId.value
        viewModelScope.launch {
            if (current != null && unbindThisDevice) {
                repository.unbindCurrentDevice(current.key, device)
            }
            _activeKey.value = null
            _authUiState.value = AuthUiState(
                successMessage = if (_isBengali.value) "লগআউট সম্পন্ন হয়েছে" else "Logged out successfully"
            )
        }
    }

    fun changeDeviceId(customId: String?) {
        val app = getApplication<Application>()
        DeviceUtils.setSimulatedDeviceId(app, customId)
        _simulatedDeviceId.value = customId?.takeIf { it.isNotBlank() }
        _currentDeviceId.value = DeviceUtils.getCurrentDeviceId(app)

        // If active session belongs to different device, re-evaluate
        if (_activeKey.value != null) {
            refreshActiveKey()
        }
    }

    fun restoreHardwareDeviceId() {
        changeDeviceId(null)
    }

    fun createKey(key: String, days: Int, maxDevices: Int, status: String, note: String) {
        viewModelScope.launch {
            val newKey = KeyItem(
                key = key.trim().uppercase(),
                days = days.coerceAtLeast(1),
                maxDevices = maxDevices.coerceAtLeast(1),
                devices = emptyList(),
                status = status,
                createdAt = System.currentTimeMillis(),
                expiresAt = null,
                isUsed = false,
                note = note
            )
            repository.saveKey(newKey)
        }
    }

    fun generateAndCreateKey(
        days: Int,
        maxDevices: Int,
        customKeyName: String? = null,
        note: String = "",
        onCreated: ((String, Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val finalKey = if (!customKeyName.isNullOrBlank()) {
                customKeyName.trim().uppercase()
            } else {
                val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                val randomSuffix = (1..8).map { chars.random() }.joinToString("")
                "KEY-$randomSuffix"
            }

            val keyItem = KeyItem(
                key = finalKey,
                days = days.coerceAtLeast(1),
                maxDevices = maxDevices.coerceAtLeast(1),
                devices = emptyList(),
                status = "active",
                createdAt = System.currentTimeMillis(),
                expiresAt = null,
                isUsed = false,
                note = note.ifBlank { "Generated via Admin Panel" }
            )
            // Instantly pushed to Firebase RTDB in repository.saveKey
            val isSynced = repository.saveKey(keyItem)
            onCreated?.invoke(finalKey, isSynced)
        }
    }

    fun generateAndCreateKey(days: Int, maxDevices: Int, onCreated: ((String) -> Unit)?) {
        generateAndCreateKey(days, maxDevices, null, "") { key, _ ->
            onCreated?.invoke(key)
        }
    }

    fun syncWithCloud() {
        viewModelScope.launch {
            repository.syncWithCloud()
        }
    }

    fun pushAllLocalToCloud() {
        viewModelScope.launch {
            repository.pushAllLocalToCloud()
        }
    }

    fun toggleKeyStatus(key: String) {
        viewModelScope.launch {
            repository.toggleKeyStatus(key)
            if (_activeKey.value?.key == key) {
                refreshActiveKey()
            }
        }
    }

    fun resetKeyDevices(key: String) {
        viewModelScope.launch {
            repository.resetDeviceBindings(key)
            if (_activeKey.value?.key == key) {
                refreshActiveKey()
            }
        }
    }

    fun deleteKey(key: String) {
        viewModelScope.launch {
            repository.deleteKey(key)
            if (_activeKey.value?.key == key) {
                _activeKey.value = null
            }
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetAllToDefaults()
            _activeKey.value = null
            _authUiState.value = AuthUiState(
                successMessage = if (_isBengali.value) "সব চাবি রিসেট করা হয়েছে" else "All keys reset to defaults"
            )
        }
    }

    fun importJson(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.importKeysFromJson(jsonString)
            result.fold(
                onSuccess = { count ->
                    onResult(true, "Successfully imported $count keys / $count টি চাবি সফলভাবে ইমপোর্ট হয়েছে")
                },
                onFailure = { error ->
                    onResult(false, "Import failed: ${error.localizedMessage ?: "Invalid JSON format"}")
                }
            )
        }
    }

    suspend fun getExportJson(): String {
        return repository.exportKeysToJson()
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun clearAuthMessages() {
        _authUiState.value = _authUiState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }

    fun loadAppStatus() {
        viewModelScope.launch {
            repository.rtdbService.getAppStatus().onSuccess { (status, notice) ->
                _isServerOnline.value = status.lowercase() != "offline"
                _maintenanceNotice.value = notice
            }
        }
    }

    fun setServerStatus(online: Boolean, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _isStatusUpdating.value = true
            val statusStr = if (online) "online" else "offline"
            val result = repository.rtdbService.setAppStatus(statusStr, _maintenanceNotice.value)
            if (result.isSuccess) {
                _isServerOnline.value = online
                onComplete?.invoke(true)
            } else {
                onComplete?.invoke(false)
            }
            _isStatusUpdating.value = false
        }
    }

    fun updateMaintenanceNotice(notice: String, onComplete: ((Boolean) -> Unit)? = null) {
        _maintenanceNotice.value = notice
        viewModelScope.launch {
            _isStatusUpdating.value = true
            val statusStr = if (_isServerOnline.value) "online" else "offline"
            val result = repository.rtdbService.setAppStatus(statusStr, notice)
            onComplete?.invoke(result.isSuccess)
            _isStatusUpdating.value = false
        }
    }
}
