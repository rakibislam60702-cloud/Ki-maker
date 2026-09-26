package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.KeyEntity
import com.example.data.local.SessionLogEntity
import com.example.data.model.KeyItem
import com.example.data.model.SessionLog
import com.example.data.remote.AppStatusInfo
import com.example.data.remote.FirebaseRtdbService
import com.example.data.remote.WorkerKeyCreationResult
import com.example.data.remote.WorkerKeyService
import com.example.util.JsonUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class AuthResult {
    data class Success(
        val keyItem: KeyItem,
        val isFirstActivation: Boolean,
        val message: String
    ) : AuthResult()

    data class Error(
        val reason: AuthErrorReason,
        val message: String
    ) : AuthResult()
}

enum class AuthErrorReason {
    NOT_FOUND,
    BLOCKED,
    MAX_DEVICES_REACHED,
    EXPIRED,
    EMPTY_KEY
}

data class CloudSyncStatus(
    val state: String = "IDLE", // "IDLE", "SYNCING", "SYNCED", "ERROR"
    val lastSyncTime: Long? = null,
    val syncedCount: Int = 0,
    val message: String = "Firebase Realtime DB Ready (DPModsSecurity)",
    val isOnline: Boolean = true
)

class KeyRepository(
    private val database: AppDatabase,
    val rtdbService: FirebaseRtdbService = FirebaseRtdbService(),
    val workerKeyService: WorkerKeyService = WorkerKeyService()
) {
    private val keyDao = database.keyDao()
    private val logDao = database.sessionLogDao()

    private val _cloudSyncStatus = MutableStateFlow(CloudSyncStatus())
    val cloudSyncStatus: StateFlow<CloudSyncStatus> = _cloudSyncStatus.asStateFlow()

    val allKeysFlow: Flow<List<KeyItem>> = keyDao.getAllKeysFlow().map { list ->
        list.map { it.toModel() }
    }

    val allLogsFlow: Flow<List<SessionLog>> = logDao.getAllLogsFlow().map { list ->
        list.map { it.toModel() }
    }

    suspend fun initializeDefaultKeysIfNeeded() {
        val count = keyDao.getAllKeys().size
        if (count == 0) {
            val defaults = JsonUtils.getDefaultSeedKeys()
            keyDao.insertAll(defaults.map { KeyEntity.fromModel(it) })
            logDao.insertLog(
                SessionLogEntity(
                    keyCode = "SYSTEM",
                    deviceId = "LOCAL_INIT",
                    timestamp = System.currentTimeMillis(),
                    action = "DB_INITIALIZED",
                    details = "Initialized database with ${defaults.size} default keys including KEY-ABC123XYZ",
                    isSuccess = true
                )
            )
        }
        // Attempt initial background cloud sync from DPModsSecurity/Keys
        syncWithCloud()
    }

    suspend fun syncWithCloud(): Result<Int> {
        _cloudSyncStatus.value = _cloudSyncStatus.value.copy(
            state = "SYNCING",
            message = "Connecting to DPModsSecurity/Keys on Firebase..."
        )
        val result = rtdbService.fetchAllKeys()
        return if (result.isSuccess) {
            val cloudKeys = result.getOrNull() ?: emptyList()
            if (cloudKeys.isNotEmpty()) {
                keyDao.insertAll(cloudKeys.map { KeyEntity.fromModel(it) })
                logDao.insertLog(
                    SessionLogEntity(
                        keyCode = "CLOUD_SYNC",
                        deviceId = "FIREBASE",
                        timestamp = System.currentTimeMillis(),
                        action = "PULL_SUCCESS",
                        details = "Pulled ${cloudKeys.size} keys from DPModsSecurity/Keys",
                        isSuccess = true
                    )
                )
                _cloudSyncStatus.value = CloudSyncStatus(
                    state = "SYNCED",
                    lastSyncTime = System.currentTimeMillis(),
                    syncedCount = cloudKeys.size,
                    message = "Synced ${cloudKeys.size} keys from DPModsSecurity/Keys",
                    isOnline = true
                )
                Result.success(cloudKeys.size)
            } else {
                // Cloud is empty, push local default keys to DPModsSecurity/Keys
                val localKeys = keyDao.getAllKeys().map { it.toModel() }
                if (localKeys.isNotEmpty()) {
                    rtdbService.seedKeys(localKeys)
                    _cloudSyncStatus.value = CloudSyncStatus(
                        state = "SYNCED",
                        lastSyncTime = System.currentTimeMillis(),
                        syncedCount = localKeys.size,
                        message = "Pushed ${localKeys.size} local keys to DPModsSecurity/Keys",
                        isOnline = true
                    )
                    Result.success(localKeys.size)
                } else {
                    _cloudSyncStatus.value = CloudSyncStatus(
                        state = "SYNCED",
                        lastSyncTime = System.currentTimeMillis(),
                        syncedCount = 0,
                        message = "Connected to DPModsSecurity/Keys (0 keys)",
                        isOnline = true
                    )
                    Result.success(0)
                }
            }
        } else {
            val err = result.exceptionOrNull()?.localizedMessage ?: "Unknown network error"
            _cloudSyncStatus.value = _cloudSyncStatus.value.copy(
                state = "ERROR",
                message = "Local mode (Offline / $err)",
                isOnline = false
            )
            Result.failure(result.exceptionOrNull() ?: Exception(err))
        }
    }

    suspend fun pushAllLocalToCloud(): Result<Int> {
        val localKeys = keyDao.getAllKeys().map { it.toModel() }
        val res = rtdbService.seedKeys(localKeys)
        if (res.isSuccess) {
            _cloudSyncStatus.value = CloudSyncStatus(
                state = "SYNCED",
                lastSyncTime = System.currentTimeMillis(),
                syncedCount = res.getOrDefault(localKeys.size),
                message = "Pushed ${localKeys.size} keys to DPModsSecurity/Keys",
                isOnline = true
            )
        }
        return res
    }

    suspend fun authenticate(rawKey: String, currentDeviceId: String): AuthResult {
        val trimmedKey = rawKey.trim().uppercase()
        if (trimmedKey.isEmpty()) {
            return AuthResult.Error(
                reason = AuthErrorReason.EMPTY_KEY,
                message = "Please enter a license key / অনুগ্রহ করে চাবি লিখুন"
            )
        }

        // Check if there's an updated state from cloud first (non-blocking failure)
        try {
            val cloudKeysResult = rtdbService.fetchAllKeys()
            if (cloudKeysResult.isSuccess) {
                val cloudMatch = cloudKeysResult.getOrNull()?.find { it.key.equals(trimmedKey, ignoreCase = true) }
                if (cloudMatch != null) {
                    keyDao.insertOrUpdate(KeyEntity.fromModel(cloudMatch))
                }
            }
        } catch (_: Exception) {}

        val entity = keyDao.getKey(trimmedKey)
        if (entity == null) {
            logDao.insertLog(
                SessionLogEntity(
                    keyCode = trimmedKey,
                    deviceId = currentDeviceId,
                    timestamp = System.currentTimeMillis(),
                    action = "AUTH_FAILED_NOT_FOUND",
                    details = "Attempted login with non-existent key",
                    isSuccess = false
                )
            )
            return AuthResult.Error(
                reason = AuthErrorReason.NOT_FOUND,
                message = "License key '$trimmedKey' was not found / এই চাবিটি সিস্টেমে নেই"
            )
        }

        val keyItem = entity.toModel()

        // 1. Status check (Banned)
        if (keyItem.isBlocked) {
            logDao.insertLog(
                SessionLogEntity(
                    keyCode = keyItem.key,
                    deviceId = currentDeviceId,
                    timestamp = System.currentTimeMillis(),
                    action = "AUTH_BLOCKED",
                    details = "Key is marked as banned by admin",
                    isSuccess = false
                )
            )
            return AuthResult.Error(
                reason = AuthErrorReason.BLOCKED,
                message = "This license key is banned / এই চাবিটি ব্যান করা আছে"
            )
        }

        // 2. Expiration check
        val now = System.currentTimeMillis()
        if (keyItem.isExpired) {
            logDao.insertLog(
                SessionLogEntity(
                    keyCode = keyItem.key,
                    deviceId = currentDeviceId,
                    timestamp = now,
                    action = "AUTH_EXPIRED",
                    details = "Key expired on ${keyItem.expiryDateStr.ifEmpty { keyItem.expiresAt.toString() }}",
                    isSuccess = false
                )
            )
            return AuthResult.Error(
                reason = AuthErrorReason.EXPIRED,
                message = "License key has expired / চাবির মেয়াদ শেষ হয়েছে"
            )
        }

        // 3. Device Binding & Max Devices check
        val isDeviceRegistered = keyItem.devices.contains(currentDeviceId)
        val updatedDevices = if (isDeviceRegistered) {
            keyItem.devices
        } else {
            if (keyItem.devices.size >= keyItem.maxDevices) {
                logDao.insertLog(
                    SessionLogEntity(
                        keyCode = keyItem.key,
                        deviceId = currentDeviceId,
                        timestamp = now,
                        action = "MAX_DEVICES_EXCEEDED",
                        details = "DeviceLimit: ${keyItem.maxDevices}, Registered: ${keyItem.devices.joinToString()}",
                        isSuccess = false
                    )
                )
                return AuthResult.Error(
                    reason = AuthErrorReason.MAX_DEVICES_REACHED,
                    message = "Maximum device limit reached (${keyItem.devices.size}/${keyItem.maxDevices}) / অনুমোদিত ডিভাইসের সংখ্যা শেষ"
                )
            } else {
                keyItem.devices + currentDeviceId
            }
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        var isFirstActivation = false
        val newExpiresAt = if (keyItem.expiresAt == null || !keyItem.isUsed) {
            isFirstActivation = true
            now + (keyItem.days * 86_400_000L)
        } else {
            keyItem.expiresAt
        }

        val finalExpiryDateStr = if (keyItem.expiryDateStr.isNotBlank()) {
            keyItem.expiryDateStr
        } else {
            sdf.format(Date(newExpiresAt))
        }

        val updatedKey = keyItem.copy(
            devices = updatedDevices,
            expiresAt = newExpiresAt,
            isUsed = true,
            expiryDateStr = finalExpiryDateStr
        )

        // Save locally
        keyDao.insertOrUpdate(KeyEntity.fromModel(updatedKey))

        // Push session updates to DPModsSecurity/Keys/{keyName}
        rtdbService.updateKeySession(
            key = updatedKey.key,
            devices = updatedKey.devices,
            expiresAt = updatedKey.expiresAt,
            isUsed = updatedKey.isUsed,
            expiryDateStr = updatedKey.expiryDateStr
        )

        val actionName = if (isFirstActivation) "FIRST_LOGIN_ACTIVATED" else "LOGIN_SUCCESS"
        val message = if (isFirstActivation) {
            "License activated! Validity until $finalExpiryDateStr / লাইসেন্স সফলভাবে সক্রিয় হয়েছে!"
        } else {
            "License verified successfully / লগইন সফল হয়েছে"
        }

        logDao.insertLog(
            SessionLogEntity(
                keyCode = updatedKey.key,
                deviceId = currentDeviceId,
                timestamp = now,
                action = actionName,
                details = "Device bound: $currentDeviceId | ExpiryDate: $finalExpiryDateStr",
                isSuccess = true
            )
        )

        return AuthResult.Success(
            keyItem = updatedKey,
            isFirstActivation = isFirstActivation,
            message = message
        )
    }

    suspend fun getKey(keyCode: String): KeyItem? {
        return keyDao.getKey(keyCode)?.toModel()
    }

    suspend fun saveKey(keyItem: KeyItem): Boolean {
        keyDao.insertOrUpdate(KeyEntity.fromModel(keyItem))
        // Instantly push to Firebase Realtime Database under DPModsSecurity/Keys/{keyName}
        val result = rtdbService.putKey(keyItem)
        return if (result.isSuccess) {
            _cloudSyncStatus.value = CloudSyncStatus(
                state = "SYNCED",
                lastSyncTime = System.currentTimeMillis(),
                message = "Live Synced: ${keyItem.key} saved to DPModsSecurity/Keys",
                isOnline = true
            )
            logDao.insertLog(
                SessionLogEntity(
                    keyCode = keyItem.key,
                    deviceId = "ADMIN",
                    timestamp = System.currentTimeMillis(),
                    action = "KEY_CREATED_CLOUD_SYNC",
                    details = "Key ${keyItem.key} (Limit: ${keyItem.maxDevices}, Expiry: ${keyItem.expiryDateStr}) pushed to DPModsSecurity/Keys",
                    isSuccess = true
                )
            )
            true
        } else {
            val err = result.exceptionOrNull()?.message ?: "Unknown error"
            _cloudSyncStatus.value = CloudSyncStatus(
                state = "ERROR",
                lastSyncTime = System.currentTimeMillis(),
                message = "Saved locally. Firebase sync pending: $err",
                isOnline = false
            )
            false
        }
    }

    /**
     * Executes the exact key deployment logic using the Cloudflare Worker API:
     * POST https://misty-bush-77a9.rakibul74348.workers.dev/api/admin/create-key
     * Payload: { key, days, deviceLimit, user: "Admin Created" }
     *
     * If worker responds with success:
     * - Saves key locally in Room DB
     * - Pushes key to Firebase RTDB under DPModsSecurity/Keys/{keyName}
     * - Returns WorkerKeyCreationResult(isSuccess = true, key = resultKey)
     */
    suspend fun deployKeyViaWorker(
        customKey: String?,
        validityDays: Int,
        deviceLimit: Int
    ): WorkerKeyCreationResult {
        val days = validityDays.coerceAtLeast(1)
        val limit = deviceLimit.coerceAtLeast(1)

        val keyName = if (!customKey.isNullOrBlank()) {
            customKey.trim().uppercase()
        } else {
            workerKeyService.generateRandomAimKey()
        }

        val workerResult = workerKeyService.createKeyViaWorker(
            key = keyName,
            days = days,
            deviceLimit = limit,
            user = "Admin Created"
        )

        if (workerResult.isSuccess) {
            val finalKey = workerResult.key?.ifBlank { keyName } ?: keyName
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val expiryDateStr = sdf.format(Date(System.currentTimeMillis() + days * 86_400_000L))

            val keyItem = KeyItem(
                key = finalKey,
                days = days,
                maxDevices = limit,
                devices = emptyList(),
                status = "active", // Banned: false
                createdAt = System.currentTimeMillis(),
                expiresAt = null,
                isUsed = false,
                note = "Worker & Firebase Synced",
                expiryDateStr = expiryDateStr
            )

            // Save locally
            keyDao.insertOrUpdate(KeyEntity.fromModel(keyItem))

            // Ensure Firebase RTDB has it under DPModsSecurity/Keys/{keyName}
            rtdbService.putKey(keyItem)

            // Audit log
            logDao.insertLog(
                SessionLogEntity(
                    keyCode = finalKey,
                    deviceId = "ADMIN",
                    timestamp = System.currentTimeMillis(),
                    action = "KEY_DEPLOYED_WORKER",
                    details = "Key $finalKey deployed via Cloudflare Worker and Firebase RTDB",
                    isSuccess = true
                )
            )

            _cloudSyncStatus.value = CloudSyncStatus(
                state = "SYNCED",
                lastSyncTime = System.currentTimeMillis(),
                message = "Live Synced: $finalKey deployed via Worker API",
                isOnline = true
            )

            return WorkerKeyCreationResult(
                isSuccess = true,
                key = finalKey
            )
        } else {
            logDao.insertLog(
                SessionLogEntity(
                    keyCode = keyName,
                    deviceId = "ADMIN",
                    timestamp = System.currentTimeMillis(),
                    action = "KEY_DEPLOY_FAILED",
                    details = "Worker deploy failed: ${workerResult.errorMessage}",
                    isSuccess = false
                )
            )
            return workerResult
        }
    }

    /**
     * Ban/Unban action: toggle "DPModsSecurity/Keys/{keyName}/Banned"
     */
    suspend fun toggleKeyStatus(keyCode: String): KeyItem? {
        val existing = keyDao.getKey(keyCode)?.toModel() ?: return null
        val willBeBlocked = existing.status.equals("active", ignoreCase = true)
        val newStatus = if (willBeBlocked) "blocked" else "active"
        val updated = existing.copy(status = newStatus)
        keyDao.insertOrUpdate(KeyEntity.fromModel(updated))

        // Sync toggle to Firebase RTDB under DPModsSecurity/Keys/{keyName}/Banned
        rtdbService.setKeyBanned(keyCode, willBeBlocked)

        logDao.insertLog(
            SessionLogEntity(
                keyCode = keyCode,
                deviceId = "ADMIN",
                timestamp = System.currentTimeMillis(),
                action = if (willBeBlocked) "KEY_BANNED" else "KEY_UNBANNED",
                details = "Key Banned set to $willBeBlocked (Synced to DPModsSecurity/Keys/$keyCode/Banned)",
                isSuccess = true
            )
        )
        return updated
    }

    suspend fun resetDeviceBindings(keyCode: String): KeyItem? {
        val existing = keyDao.getKey(keyCode)?.toModel() ?: return null
        val updated = existing.copy(
            devices = emptyList(),
            expiresAt = null,
            isUsed = false
        )
        keyDao.insertOrUpdate(KeyEntity.fromModel(updated))

        // Sync reset to DPModsSecurity/Keys
        rtdbService.updateKeySession(keyCode, emptyList(), null, false, existing.expiryDateStr)

        logDao.insertLog(
            SessionLogEntity(
                keyCode = keyCode,
                deviceId = "ADMIN",
                timestamp = System.currentTimeMillis(),
                action = "DEVICES_RESET",
                details = "Cleared bound devices from DPModsSecurity/Keys/$keyCode",
                isSuccess = true
            )
        )
        return updated
    }

    suspend fun unbindCurrentDevice(keyCode: String, deviceId: String): KeyItem? {
        val existing = keyDao.getKey(keyCode)?.toModel() ?: return null
        val updatedDevices = existing.devices.filter { it != deviceId }
        val updated = existing.copy(devices = updatedDevices)
        keyDao.insertOrUpdate(KeyEntity.fromModel(updated))

        // Sync to DPModsSecurity/Keys
        rtdbService.updateKeySession(keyCode, updatedDevices, existing.expiresAt, existing.isUsed, existing.expiryDateStr)

        logDao.insertLog(
            SessionLogEntity(
                keyCode = keyCode,
                deviceId = deviceId,
                timestamp = System.currentTimeMillis(),
                action = "DEVICE_UNBOUND",
                details = "Unbound device $deviceId from DPModsSecurity/Keys/$keyCode",
                isSuccess = true
            )
        )
        return updated
    }

    /**
     * Delete action: remove the node "DPModsSecurity/Keys/{keyName}"
     */
    suspend fun deleteKey(keyCode: String) {
        keyDao.deleteByKey(keyCode)
        // Sync delete to DPModsSecurity/Keys/{keyName}
        rtdbService.deleteKey(keyCode)

        logDao.insertLog(
            SessionLogEntity(
                keyCode = keyCode,
                deviceId = "ADMIN",
                timestamp = System.currentTimeMillis(),
                action = "KEY_DELETED",
                details = "License key removed from DPModsSecurity/Keys/$keyCode",
                isSuccess = true
            )
        )
    }

    suspend fun resetAllToDefaults() {
        keyDao.clearAll()
        val defaults = JsonUtils.getDefaultSeedKeys()
        keyDao.insertAll(defaults.map { KeyEntity.fromModel(it) })
        rtdbService.seedKeys(defaults)

        logDao.insertLog(
            SessionLogEntity(
                keyCode = "ALL",
                deviceId = "ADMIN",
                timestamp = System.currentTimeMillis(),
                action = "RESET_TO_DEFAULTS",
                details = "Restored all default keys to Room and DPModsSecurity/Keys",
                isSuccess = true
            )
        )
    }

    suspend fun importKeysFromJson(jsonString: String): Result<Int> {
        return try {
            val parsed = JsonUtils.parseJson(jsonString)
            if (parsed.isEmpty()) {
                Result.failure(Exception("No valid keys found in JSON"))
            } else {
                keyDao.insertAll(parsed.map { KeyEntity.fromModel(it) })
                rtdbService.seedKeys(parsed)
                logDao.insertLog(
                    SessionLogEntity(
                        keyCode = "IMPORT",
                        deviceId = "ADMIN",
                        timestamp = System.currentTimeMillis(),
                        action = "KEYS_IMPORTED",
                        details = "Imported ${parsed.size} keys to DPModsSecurity/Keys",
                        isSuccess = true
                    )
                )
                Result.success(parsed.size)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportKeysToJson(): String {
        val all = keyDao.getAllKeys().map { it.toModel() }
        return JsonUtils.toJson(all)
    }

    suspend fun clearLogs() {
        logDao.clearLogs()
    }
}
