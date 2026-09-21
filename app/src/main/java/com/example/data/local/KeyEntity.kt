package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.KeyItem
import org.json.JSONArray

@Entity(tableName = "license_keys")
data class KeyEntity(
    @PrimaryKey
    val keyCode: String,
    val days: Int,
    val maxDevices: Int,
    val devicesJson: String, // Stored as JSON array string
    val status: String,
    val createdAt: Long,
    val expiresAt: Long?,
    val isUsed: Boolean,
    val note: String = ""
) {
    fun toModel(): KeyItem {
        val deviceList = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(devicesJson)
            for (i in 0 until jsonArray.length()) {
                deviceList.add(jsonArray.getString(i))
            }
        } catch (_: Exception) {}

        return KeyItem(
            key = keyCode,
            days = days,
            maxDevices = maxDevices,
            devices = deviceList,
            status = status,
            createdAt = createdAt,
            expiresAt = expiresAt,
            isUsed = isUsed,
            note = note
        )
    }

    companion object {
        fun fromModel(model: KeyItem): KeyEntity {
            val jsonArray = JSONArray()
            model.devices.forEach { jsonArray.put(it) }
            return KeyEntity(
                keyCode = model.key,
                days = model.days,
                maxDevices = model.maxDevices,
                devicesJson = jsonArray.toString(),
                status = model.status,
                createdAt = model.createdAt,
                expiresAt = model.expiresAt,
                isUsed = model.isUsed,
                note = model.note
            )
        }
    }
}
