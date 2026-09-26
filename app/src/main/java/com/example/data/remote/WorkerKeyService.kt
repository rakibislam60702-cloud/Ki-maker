package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Result model representing response from the Cloudflare Worker key creation API:
 * POST https://misty-bush-77a9.rakibul74348.workers.dev/api/admin/create-key
 */
data class WorkerKeyCreationResult(
    val isSuccess: Boolean,
    val key: String? = null,
    val errorMessage: String? = null,
    val isConnectionError: Boolean = false
)

class WorkerKeyService(
    val endpointUrl: String = "https://misty-bush-77a9.rakibul74348.workers.dev/api/admin/create-key"
) {
    private val tag = "WorkerKeyService"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Generates a random key matching:
     * "AIM-" + Math.random().toString(36).substring(2, 10).toUpperCase()
     */
    fun generateRandomAimKey(): String {
        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val randomSuffix = (1..8).map { chars.random() }.joinToString("")
        return "AIM-$randomSuffix"
    }

    /**
     * Executes the exact endpoint call:
     * POST https://misty-bush-77a9.rakibul74348.workers.dev/api/admin/create-key
     * Headers: Content-Type: application/json
     * Body: {
     *   "key": keyName,
     *   "days": validityDays,
     *   "deviceLimit": deviceLimit,
     *   "user": "Admin Created"
     * }
     */
    suspend fun createKeyViaWorker(
        key: String,
        days: Int,
        deviceLimit: Int,
        user: String = "Admin Created"
    ): WorkerKeyCreationResult = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("key", key)
                put("days", days)
                put("deviceLimit", deviceLimit)
                put("user", user)
            }

            val requestBody = payload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(endpointUrl)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build()

            Log.d(tag, "Sending request to $endpointUrl with payload: $payload")

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()?.trim().orEmpty()
                Log.d(tag, "Response code: ${response.code}, body: $responseBody")

                if (responseBody.isBlank()) {
                    return@withContext WorkerKeyCreationResult(
                        isSuccess = false,
                        errorMessage = "Empty response from server (HTTP ${response.code})"
                    )
                }

                try {
                    val json = JSONObject(responseBody)
                    val isSuccess = json.optBoolean("success", false)

                    if (isSuccess) {
                        val returnedKey = json.optString("key", key).ifBlank { key }
                        WorkerKeyCreationResult(
                            isSuccess = true,
                            key = returnedKey
                        )
                    } else {
                        val error = json.optString("error", "Failed to create key")
                        WorkerKeyCreationResult(
                            isSuccess = false,
                            errorMessage = error
                        )
                    }
                } catch (parseEx: Exception) {
                    Log.w(tag, "JSON parse warning: ${parseEx.message}")
                    if (response.isSuccessful) {
                        WorkerKeyCreationResult(
                            isSuccess = true,
                            key = key
                        )
                    } else {
                        WorkerKeyCreationResult(
                            isSuccess = false,
                            errorMessage = "HTTP ${response.code}: ${response.message}"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Connection error: ${e.message}", e)
            WorkerKeyCreationResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: e.message ?: "Network connection failed",
                isConnectionError = true
            )
        }
    }
}
