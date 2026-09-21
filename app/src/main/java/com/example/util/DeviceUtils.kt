package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import java.util.UUID

object DeviceUtils {
    private const val PREFS_NAME = "key_auth_device_prefs"
    private const val KEY_SIMULATED_DEVICE_ID = "simulated_device_id"
    private const val KEY_HARDWARE_UUID = "hardware_fallback_uuid"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @SuppressLint("HardwareIds")
    fun getHardwareDeviceId(context: Context): String {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Exception) {
            null
        }

        if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
            return "DEV-${androidId.takeLast(8).uppercase()}"
        }

        val prefs = getPrefs(context)
        var fallback = prefs.getString(KEY_HARDWARE_UUID, null)
        if (fallback == null) {
            fallback = "DEV-" + UUID.randomUUID().toString().take(8).uppercase()
            prefs.edit().putString(KEY_HARDWARE_UUID, fallback).apply()
        }
        return fallback
    }

    fun getCurrentDeviceId(context: Context): String {
        val simulated = getSimulatedDeviceId(context)
        return if (!simulated.isNullOrBlank()) {
            simulated
        } else {
            getHardwareDeviceId(context)
        }
    }

    fun getSimulatedDeviceId(context: Context): String? {
        return getPrefs(context).getString(KEY_SIMULATED_DEVICE_ID, null)
    }

    fun setSimulatedDeviceId(context: Context, deviceId: String?) {
        val editor = getPrefs(context).edit()
        if (deviceId.isNullOrBlank()) {
            editor.remove(KEY_SIMULATED_DEVICE_ID)
        } else {
            editor.putString(KEY_SIMULATED_DEVICE_ID, deviceId.trim())
        }
        editor.apply()
    }

    fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "Device Model" to "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            "Android Version" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "Hardware" to Build.HARDWARE,
            "Board" to Build.BOARD,
            "Security Patch" to (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A"),
            "Arch" to Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
        )
    }
}
