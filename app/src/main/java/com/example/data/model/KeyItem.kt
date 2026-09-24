package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class KeyItem(
    val key: String,
    val days: Int,
    val maxDevices: Int,
    val devices: List<String>,
    val status: String, // "active" or "blocked"
    val createdAt: Long,
    val expiresAt: Long? = null,
    val isUsed: Boolean = false,
    val note: String = "",
    val expiryDateStr: String = "" // Strict YYYY-MM-DD format as in Firebase schema
) {
    val isActive: Boolean
        get() = status.equals("active", ignoreCase = true)

    val isBlocked: Boolean
        get() = status.equals("blocked", ignoreCase = true)

    val isExpired: Boolean
        get() {
            if (expiresAt != null && System.currentTimeMillis() > expiresAt) return true
            if (expiryDateStr.isNotBlank()) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val parsedDate = sdf.parse(expiryDateStr)
                    if (parsedDate != null) {
                        // End of day
                        val endOfDay = parsedDate.time + 86_400_000L - 1
                        return System.currentTimeMillis() > endOfDay
                    }
                } catch (_: Exception) {}
            }
            return false
        }

    fun getComputedExpiresAt(): Long? {
        if (expiresAt != null) return expiresAt
        if (expiryDateStr.isNotBlank()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val parsedDate = sdf.parse(expiryDateStr)
                if (parsedDate != null) {
                    return parsedDate.time + 86_400_000L - 1
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun remainingTimeMillis(): Long {
        val targetExp = getComputedExpiresAt() ?: return days * 86_400_000L
        val diff = targetExp - System.currentTimeMillis()
        return if (diff > 0) diff else 0L
    }

    fun formatRemainingTime(): String {
        val targetExp = getComputedExpiresAt()
        if (targetExp == null) {
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
        val targetExp = getComputedExpiresAt() ?: return 1.0f
        val totalDuration = days * 86_400_000L
        val remaining = remainingTimeMillis()
        if (totalDuration <= 0) return 0f
        return (remaining.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    }
}
