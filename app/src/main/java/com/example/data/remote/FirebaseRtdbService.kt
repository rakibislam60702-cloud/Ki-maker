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
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

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

    suspend fun fetchAllKeys(): Result<List<KeyItem>> = withContext(Dispatchers.IO) {
        try {
            val url = "$databaseUrl/keys.json"
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
            Log.e(tag, "Failed to fetch keys from Firebase RTDB: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun putKey(keyItem: KeyItem): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "$databaseUrl/keys/${keyItem.key}.json"
            val payload = JSONObject().apply {
                put("days", keyItem.days)
                put("maxDevices", keyItem.maxDevices)
                val devicesArr = JSONArray()
                keyItem.devices.forEach { devicesArr.put(it) }
                put("devices", devicesArr)
                put("status", keyItem.status)
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

    suspend fun updateKeyStatus(key: String, newStatus: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "$databaseUrl/keys/$key.json"
            val payload = JSONObject().apply {
                put("status", newStatus)
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
            Log.e(tag, "Failed to update key status: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateKeySession(
        key: String,
        devices: List<String>,
        expiresAt: Long?,
        isUsed: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "$databaseUrl/keys/$key.json"
            val payload = JSONObject().apply {
                val devicesArr = JSONArray()
                devices.forEach { devicesArr.put(it) }
                put("devices", devicesArr)
                if (expiresAt != null) {
                    put("expiresAt", expiresAt)
                } else {
                    put("expiresAt", JSONObject.NULL)
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

    suspend fun deleteKey(key: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "$databaseUrl/keys/$key.json"
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

    suspend fun getAppStatus(): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val url = "$databaseUrl/settings.json"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string()?.trim()
                if (body.isNullOrBlank() || body == "null") {
                    return@withContext Result.success(Pair("online", ""))
                }
                val json = JSONObject(body)
                val status = json.optString("app_status", "online")
                val notice = json.optString("maintenance_notice", "")
                Result.success(Pair(status, notice))
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to get app_status: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun setAppStatus(status: String, notice: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "$databaseUrl/settings.json"
            val payload = JSONObject().apply {
                put("app_status", status)
                put("maintenance_notice", notice)
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
            Log.e(tag, "Failed to update app_status: ${e.message}")
            Result.failure(e)
        }
    }
}
