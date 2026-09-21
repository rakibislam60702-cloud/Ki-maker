package com.example.util

import com.example.data.model.KeyItem
import org.json.JSONArray
import org.json.JSONObject

object JsonUtils {

    fun getDefaultSeedKeys(): List<KeyItem> {
        return listOf(
            KeyItem(
                key = "KEY-ABC123XYZ",
                days = 3,
                maxDevices = 1,
                devices = listOf("DeviceID_1"),
                status = "active",
                createdAt = 1726927200000L,
                expiresAt = null,
                isUsed = false,
                note = "User defined default key with 3 days validity"
            ),
            KeyItem(
                key = "VIP-7DAYS-PASS",
                days = 7,
                maxDevices = 2,
                devices = emptyList(),
                status = "active",
                createdAt = System.currentTimeMillis() - 86_400_000L,
                expiresAt = null,
                isUsed = false,
                note = "7 days multi-device key (max 2 devices)"
            ),
            KeyItem(
                key = "TRIAL-24H-TEST",
                days = 1,
                maxDevices = 1,
                devices = emptyList(),
                status = "active",
                createdAt = System.currentTimeMillis(),
                expiresAt = null,
                isUsed = false,
                note = "1 day trial key"
            ),
            KeyItem(
                key = "BLOCKED-DEMO-KEY",
                days = 30,
                maxDevices = 1,
                devices = emptyList(),
                status = "blocked",
                createdAt = System.currentTimeMillis() - 172_800_000L,
                expiresAt = null,
                isUsed = false,
                note = "Sample blocked key for testing"
            ),
            KeyItem(
                key = "EXPIRED-DEMO-KEY",
                days = 1,
                maxDevices = 1,
                devices = listOf("DeviceID_1"),
                status = "active",
                createdAt = System.currentTimeMillis() - (10 * 86_400_000L),
                expiresAt = System.currentTimeMillis() - (9 * 86_400_000L),
                isUsed = true,
                note = "Sample expired key for testing"
            )
        )
    }

    /**
     * Serializes keys into the exact JSON format requested:
     * {
     *   "keys": {
     *     "KEY-ABC123XYZ": {
     *       "days": 3,
     *       "maxDevices": 1,
     *       "devices": ["DeviceID_1"],
     *       "status": "active",
     *       "createdAt": 1726927200000,
     *       "expiresAt": null,
     *       "isUsed": false
     *     }
     *   }
     * }
     */
    fun toJson(keys: List<KeyItem>): String {
        val root = JSONObject()
        val keysObj = JSONObject()

        for (item in keys) {
            val itemObj = JSONObject()
            itemObj.put("days", item.days)
            itemObj.put("maxDevices", item.maxDevices)

            val devicesArray = JSONArray()
            item.devices.forEach { devicesArray.put(it) }
            itemObj.put("devices", devicesArray)

            itemObj.put("status", item.status)
            itemObj.put("createdAt", item.createdAt)
            if (item.expiresAt != null) {
                itemObj.put("expiresAt", item.expiresAt)
            } else {
                itemObj.put("expiresAt", JSONObject.NULL)
            }
            itemObj.put("isUsed", item.isUsed)
            if (item.note.isNotBlank()) {
                itemObj.put("note", item.note)
            }

            keysObj.put(item.key, itemObj)
        }

        root.put("keys", keysObj)
        return root.toString(2)
    }

    /**
     * Parses JSON in the format { "keys": { "KEY-XYZ": { ... } } } or flat map.
     */
    fun parseJson(jsonString: String): List<KeyItem> {
        val list = mutableListOf<KeyItem>()
        val root = JSONObject(jsonString)
        val keysObj = if (root.has("keys")) root.getJSONObject("keys") else root

        val iterator = keysObj.keys()
        while (iterator.hasNext()) {
            val keyName = iterator.next()
            val itemObj = keysObj.getJSONObject(keyName)

            val days = itemObj.optInt("days", 1)
            val maxDevices = itemObj.optInt("maxDevices", 1)

            val devices = mutableListOf<String>()
            val devicesArray = itemObj.optJSONArray("devices")
            if (devicesArray != null) {
                for (i in 0 until devicesArray.length()) {
                    devices.add(devicesArray.getString(i))
                }
            }

            val status = itemObj.optString("status", "active")
            val createdAt = itemObj.optLong("createdAt", System.currentTimeMillis())
            val expiresAt = if (itemObj.isNull("expiresAt")) null else itemObj.optLong("expiresAt")
            val isUsed = itemObj.optBoolean("isUsed", false)
            val note = itemObj.optString("note", "")

            list.add(
                KeyItem(
                    key = keyName,
                    days = days,
                    maxDevices = maxDevices,
                    devices = devices,
                    status = status,
                    createdAt = createdAt,
                    expiresAt = expiresAt,
                    isUsed = isUsed,
                    note = note
                )
            )
        }
        return list
    }
}
