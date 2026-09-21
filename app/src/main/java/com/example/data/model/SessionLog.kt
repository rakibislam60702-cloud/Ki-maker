package com.example.data.model

data class SessionLog(
    val id: Long = 0,
    val key: String,
    val deviceId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String, // "LOGIN_SUCCESS", "FIRST_ACTIVATION", "BLOCKED_ATTEMPT", "MAX_DEVICES_EXCEEDED", "KEY_EXPIRED", "LOGOUT"
    val details: String,
    val isSuccess: Boolean = true
)
