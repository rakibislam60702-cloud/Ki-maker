package com.example.data.model

data class KeyItem(
    val key: String,
    val days: Int,
    val maxDevices: Int,
    val devices: List<String>,
    val status: String, // "active" or "blocked"
    val createdAt: Long,
    val expiresAt: Long? = null,
    val isUsed: Boolean = false,
    val note: String = ""
) {
    val isActive: Boolean
        get() = status.equals("active", ignoreCase = true)

    val isBlocked: Boolean
        get() = status.equals("blocked", ignoreCase = true)

    val isExpired: Boolean
        get() = expiresAt != null && System.currentTimeMillis() > expiresAt

    fun remainingTimeMillis(): Long {
        if (expiresAt == null) return days * 86_400_000L
        val diff = expiresAt - System.currentTimeMillis()
        return if (diff > 0) diff else 0L
    }

    fun formatRemainingTime(): String {
        if (expiresAt == null) {
            return "$days days (Not activated yet)"
        }
        val remaining = remainingTimeMillis()
        if (remaining <= 0) return "Expired"
        val totalSecs = remaining / 1000
        val daysLeft = totalSecs / 86400
        val hoursLeft = (totalSecs % 86400) / 3600
        val minutesLeft = (totalSecs % 3600) / 60
        val secondsLeft = totalSecs % 60
        return when {
            daysLeft > 0 -> "${daysLeft}d ${hoursLeft}h ${minutesLeft}m ${secondsLeft}s"
            hoursLeft > 0 -> "${hoursLeft}h ${minutesLeft}m ${secondsLeft}s"
            else -> "${minutesLeft}m ${secondsLeft}s"
        }
    }

    fun progressFraction(): Float {
        if (expiresAt == null) return 1.0f
        val totalDuration = days * 86_400_000L
        val remaining = remainingTimeMillis()
        if (totalDuration <= 0) return 0f
        return (remaining.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    }
}
