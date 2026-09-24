package com.example.data.remote

import android.util.Log
import com.example.data.model.KeyItem
import com.example.util.JsonUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AppStatusInfo(
    val maintenance: Boolean = false,
    val updateRequired: Boolean = false,
    val notice: String = ""
)

class FirebaseRtdbService(
    val databaseUrl: String = "https://rakib-ai-engine-default-rtdb.firebaseio.com"
) {
    private val tag = "FirebaseRtdbService"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    private fun cleanBaseUrl(): String = databaseUrl.trimEnd('/')

    /**
     * Fetch all keys strictly from: DPModsSecurity/Keys
     */
    suspend fun fetchAllKeys(): Result<List<KeyItem>> = withContext(Dispatchers.IO) {
        try {
            val url = "${cleanBaseUrl()}/DPModsSecurity/Keys.json"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string()?.trim()
                if (body.isNullOrBlank() || body == "null") {
                    return@withContext Result.success(emptyList())
                }
                val list = JsonUtils.parseJson(body)
                Result.success(list)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch keys from DPModsSecurity/Keys: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Write key strictly under: DPModsSecurity/Keys/{keyName}
     * Fields:
     * - "Banned": false (boolean)
     * - "DeviceLimit": integer
     * - "ExpiryDate": string in strict "YYYY-MM-DD" format
     * - "Devices": { "dummy": true } (object so client devices can register)
     * Plus optional backward-compatible/admin fields (days, createdAt, etc.)
     */
    suspend fun putKey(keyItem: KeyItem): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${cleanBaseUrl()}/DPModsSecurity/Keys/${keyItem.key}.json"

            // Compute strict YYYY-MM-DD format
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val expiryDateStr = if (keyItem.expiryDateStr.isNotBlank()) {
                keyItem.expiryDateStr
            } else if (keyItem.expiresAt != null) {
                dateFormat.format(Date(keyItem.expiresAt))
            } else {
                val validityDays = keyItem.days.coerceAtLeast(1)
                dateFormat.format(Date(System.currentTimeMillis() + validityDays * 86_400_000L))
            }

            val payload = JSONObject().apply {
                // Exact client app schema requirements:
                put("Banned", keyItem.isBlocked)
                put("DeviceLimit", keyItem.maxDevices)
                put("ExpiryDate", expiryDateStr)

                val devicesObj = JSONObject()
                if (keyItem.devices.isEmpty()) {
                    devicesObj.put("dummy", true)
                } else {
                    keyItem.devices.forEach { devId ->
                        devicesObj.put(devId, true)
                    }
                }
                put("Devices", devicesObj)

                // Additional metadata preserved for Admin Panel rich dashboard display
                put("days", keyItem.days)
                put("createdAt", keyItem.createdAt)
                if (keyItem.expiresAt != null) {
                    put("expiresAt", keyItem.expiresAt)
                } else {
                    put("expiresAt", JSONObject.NULL)
                }
                put("isUsed", keyItem.isUsed)
                if (keyItem.note.isNotBlank()) {
                    put("note", keyItem.note)
                }
            }

            val requestBody = payload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .put(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to save key to Firebase: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Ban / Unban action: toggle "DPModsSecurity/Keys/{keyName}/Banned"
     * Also updates "status" for backward-compatibility with UI if needed.
     */
    suspend fun setKeyBanned(key: String, banned: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${cleanBaseUrl()}/DPModsSecurity/Keys/$key.json"
            val payload = JSONObject().apply {
                put("Banned", banned)
                put("status", if (banned) "blocked" else "active")
            }

            val requestBody = payload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .patch(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to update banned status: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update key session (registered devices and expiration date) under DPModsSecurity/Keys/{keyName}
     */
    suspend fun updateKeySession(
        key: String,
        devices: List<String>,
        expiresAt: Long?,
        isUsed: Boolean,
        expiryDateStr: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${cleanBaseUrl()}/DPModsSecurity/Keys/$key.json"
            val payload = JSONObject().apply {
                val devicesObj = JSONObject()
                if (devices.isEmpty()) {
                    devicesObj.put("dummy", true)
                } else {
                    devices.forEach { devId -> devicesObj.put(devId, true) }
                }
                put("Devices", devicesObj)

                if (expiresAt != null) {
                    put("expiresAt", expiresAt)
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    put("ExpiryDate", expiryDateStr ?: dateFormat.format(Date(expiresAt)))
                }
                put("isUsed", isUsed)
            }

            val requestBody = payload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .patch(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to update key session: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete action: remove the node "DPModsSecurity/Keys/{keyName}"
     */
    suspend fun deleteKey(key: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${cleanBaseUrl()}/DPModsSecurity/Keys/$key.json"
            val request = Request.Builder()
                .url(url)
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete key: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun seedKeys(keys: List<KeyItem>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var successCount = 0
            for (k in keys) {
                if (putKey(k).isSuccess) {
                    successCount++
                }
            }
            Result.success(successCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Read app status from: DPModsSecurity/AppStatus
     * Fields: Maintenance (boolean), UpdateRequired (boolean)
     */
    suspend fun getAppStatus(): Result<AppStatusInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "${cleanBaseUrl()}/DPModsSecurity/AppStatus.json"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string()?.trim()
                if (body.isNullOrBlank() || body == "null") {
                    return@withContext Result.success(AppStatusInfo(maintenance = false, updateRequired = false, notice = ""))
                }
                val json = JSONObject(body)
                val maintenance = json.optBoolean("Maintenance", json.optBoolean("maintenance", false))
                val updateRequired = json.optBoolean("UpdateRequired", json.optBoolean("updateRequired", false))
                val notice = json.optString("Notice", json.optString("maintenance_notice", ""))
                Result.success(AppStatusInfo(maintenance = maintenance, updateRequired = updateRequired, notice = notice))
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to get DPModsSecurity/AppStatus: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * App Status management: read and toggle
     * "DPModsSecurity/AppStatus/Maintenance" and "DPModsSecurity/AppStatus/UpdateRequired"
     */
    suspend fun setAppStatus(
        maintenance: Boolean,
        updateRequired: Boolean,
        notice: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${cleanBaseUrl()}/DPModsSecurity/AppStatus.json"
            val payload = JSONObject().apply {
                put("Maintenance", maintenance)
                put("UpdateRequired", updateRequired)
                if (notice.isNotBlank()) {
                    put("Notice", notice)
                }
                put("updated_at", System.currentTimeMillis())
            }
            val requestBody = payload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .patch(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to update DPModsSecurity/AppStatus: ${e.message}")
            Result.failure(e)
        }
    }
}
