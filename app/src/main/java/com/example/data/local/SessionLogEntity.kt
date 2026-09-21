package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.SessionLog

@Entity(tableName = "session_logs")
data class SessionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val keyCode: String,
    val deviceId: String,
    val timestamp: Long,
    val action: String,
    val details: String,
    val isSuccess: Boolean
) {
    fun toModel(): SessionLog = SessionLog(
        id = id,
        key = keyCode,
        deviceId = deviceId,
        timestamp = timestamp,
        action = action,
        details = details,
        isSuccess = isSuccess
    )

    companion object {
        fun fromModel(model: SessionLog): SessionLogEntity = SessionLogEntity(
            id = model.id,
            keyCode = model.key,
            deviceId = model.deviceId,
            timestamp = model.timestamp,
            action = model.action,
            details = model.details,
            isSuccess = model.isSuccess
        )
    }
}
