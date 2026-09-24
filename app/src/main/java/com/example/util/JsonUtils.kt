package com.example.util

import com.example.data.model.KeyItem
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object JsonUtils {

    fun getDefaultSeedKeys(): List<KeyItem> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val now = System.currentTimeMillis()
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
                note = "User defined default key with 3 days validity",
                expiryDateStr = sdf.format(Date(now + 3 * 86_400_000L))
            ),
            KeyItem(
                key = "VIP-7DAYS-PASS",
                days = 7,
                maxDevices = 2,
                devices = emptyList(),
                status = "active",
                createdAt = now - 86_400_000L,
                expiresAt = null,
                isUsed = false,
                note = "7 days multi-device key (max 2 devices)",
                expiryDateStr = sdf.format(Date(now + 7 * 86_400_000L))
            ),
            KeyItem(
                key = "TRIAL-24H-TEST",
                days = 1,
                maxDevices = 1,
                devices = emptyList(),
                status = "active",
                createdAt = now,
                expiresAt = null,
                isUsed = false,
                note = "1 day trial key",
                expiryDateStr = sdf.format(Date(now + 1 * 86_400_000L))
            ),
            KeyItem(
                key = "BLOCKED-DEMO-KEY",
                days = 30,
                maxDevices = 1,
                devices = emptyList(),
                status = "blocked",
                createdAt = now - 172_800_000L,
                expiresAt = null,
                isUsed = false,
                note = "Sample blocked key for testing",
                expiryDateStr = sdf.format(Date(now + 30 * 86_400_000L))
            ),
            KeyItem(
                key = "EXPIRED-DEMO-KEY",
                days = 1,
                maxDevices = 1,
                devices = listOf("DeviceID_1"),
                status = "active",
                createdAt = now - (10 * 86_400_000L),
                expiresAt = now - (9 * 86_400_000L),
                isUsed = true,
                note = "Sample expired key for testing",
                expiryDateStr = sdf.format(Date(now - (9 * 86_400_000L)))
            )
        )
    }

    /**
     * Serializes keys into the exact JSON format requested:
     * {
     *   "DPModsSecurity": {
     *     "Keys": {
     *       "KEY-ABC123XYZ": {
     *         "Banned": false,
     *         "DeviceLimit": 1,
     *         "ExpiryDate": "2026-10-01",
     *         "Devices": { "dummy": true }
     *       }
     *     }
     *   }
     * }
     */
    fun toJson(keys: List<KeyItem>): String {
        val root = JSONObject()
        val dpMods = JSONObject()
        val keysObj = JSONObject()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        for (item in keys) {
            val itemObj = JSONObject()
            itemObj.put("Banned", item.isBlocked)
            itemObj.put("DeviceLimit", item.maxDevices)

            val expiryDateStr = if (item.expiryDateStr.isNotBlank()) {
                item.expiryDateStr
            } else if (item.expiresAt != null) {
                sdf.format(Date(item.expiresAt))
            } else {
                sdf.format(Date(System.currentTimeMillis() + item.days.coerceAtLeast(1) * 86_400_000L))
            }
            itemObj.put("ExpiryDate", expiryDateStr)

            val devicesObj = JSONObject()
            if (item.devices.isEmpty()) {
                devicesObj.put("dummy", true)
            } else {
                item.devices.forEach { devicesObj.put(it, true) }
            }
            itemObj.put("Devices", devicesObj)

            // Additional fields preserved
            itemObj.put("days", item.days)
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

        dpMods.put("Keys", keysObj)
        root.put("DPModsSecurity", dpMods)
        return root.toString(2)
    }

    /**
     * Parses JSON in the format:
     * - { "DPModsSecurity": { "Keys": { ... } } }
     * - { "Keys": { ... } }
     * - { "keys": { ... } }
     * - Flat map { "KEY-1": { ... }, "KEY-2": { ... } }
     */
    fun parseJson(jsonString: String): List<KeyItem> {
        val list = mutableListOf<KeyItem>()
        val root = JSONObject(jsonString)

        val keysObj = when {
            root.has("DPModsSecurity") -> {
                val dp = root.getJSONObject("DPModsSecurity")
                if (dp.has("Keys")) dp.getJSONObject("Keys") else dp
            }
            root.has("Keys") -> root.getJSONObject("Keys")
            root.has("keys") -> root.getJSONObject("keys")
            else -> root
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val iterator = keysObj.keys()
        while (iterator.hasNext()) {
            val keyName = iterator.next()
            val itemObj = keysObj.optJSONObject(keyName) ?: continue

            // 1. Device limit
            val deviceLimit = itemObj.optInt("DeviceLimit", itemObj.optInt("maxDevices", 1))

            // 2. Banned / status
            val isBanned = if (itemObj.has("Banned")) {
                itemObj.optBoolean("Banned", false)
            } else {
                itemObj.optString("status", "active").equals("blocked", ignoreCase = true)
            }
            val status = if (isBanned) "blocked" else "active"

            // 3. Expiry date string
            var expiryDateStr = itemObj.optString("ExpiryDate", "")
            var expiresAt: Long? = if (itemObj.isNull("expiresAt")) null else itemObj.optLong("expiresAt")
            if (expiryDateStr.isNotBlank() && expiresAt == null) {
                try {
                    val parsed = sdf.parse(expiryDateStr)
                    if (parsed != null) {
                        expiresAt = parsed.time + 86_400_000L - 1
                    }
                } catch (_: Exception) {}
            }

            // 4. Devices parsing (supports either object { "dev1": true, "dummy": true } or array ["dev1"])
            val devices = mutableListOf<String>()
            val devicesObj = itemObj.optJSONObject("Devices")
            if (devicesObj != null) {
                val devIter = devicesObj.keys()
                while (devIter.hasNext()) {
                    val dev = devIter.next()
                    if (!dev.equals("dummy", ignoreCase = true) && dev.isNotBlank()) {
                        devices.add(dev)
                    }
                }
            } else {
                val devicesArray = itemObj.optJSONArray("devices")
                if (devicesArray != null) {
                    for (i in 0 until devicesArray.length()) {
                        val d = devicesArray.getString(i)
                        if (!d.equals("dummy", ignoreCase = true)) {
                            devices.add(d)
                        }
                    }
                }
            }

            // 5. Days
            val days = itemObj.optInt("days", 3)
            val createdAt = itemObj.optLong("createdAt", System.currentTimeMillis())
            val isUsed = itemObj.optBoolean("isUsed", devices.isNotEmpty())
            val note = itemObj.optString("note", "")

            if (expiryDateStr.isBlank()) {
                val validDays = days.coerceAtLeast(1)
                expiryDateStr = sdf.format(Date(System.currentTimeMillis() + validDays * 86_400_000L))
            }

            list.add(
                KeyItem(
                    key = keyName,
                    days = days,
                    maxDevices = deviceLimit,
                    devices = devices,
                    status = status,
                    createdAt = createdAt,
                    expiresAt = expiresAt,
                    isUsed = isUsed,
                    note = note,
                    expiryDateStr = expiryDateStr
                )
            )
        }

        return list
    }
}
