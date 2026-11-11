package com.familysafety.guard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.familysafety.guard.data.model.TelemetryPayload

@Entity(tableName = "offline_location_queue")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val deviceModel: String,
    val latitude: Double,
    val longitude: Double,
    val isCharging: Boolean,
    val batteryLevel: Int,
    val signalStrength: Int,
    val networkType: String,
    val osVersion: String,
    val locale: String,
    val uptimeMs: Long,
    val screenBrightness: Int,
    val remarks: String,
    val sosFlag: Boolean,
    val status: Int,
    val timestampIso: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
) {
    fun toPayload(): TelemetryPayload {
        return TelemetryPayload(
            username = username,
            deviceModel = deviceModel,
            latitude = latitude,
            longitude = longitude,
            isCharging = isCharging,
            batteryLevel = batteryLevel,
            signalStrength = signalStrength,
            networkType = networkType,
            osVersion = osVersion,
            locale = locale,
            uptimeMs = uptimeMs,
            screenBrightness = screenBrightness,
            remarks = remarks,
            sosFlag = sosFlag,
            status = status,
            timestamp = timestampIso
        )
    }

    companion object {
        fun fromPayload(payload: TelemetryPayload): LocationEntity {
            return LocationEntity(
                username = payload.username,
                deviceModel = payload.deviceModel,
                latitude = payload.latitude,
                longitude = payload.longitude,
                isCharging = payload.isCharging,
                batteryLevel = payload.batteryLevel,
                signalStrength = payload.signalStrength,
                networkType = payload.networkType,
                osVersion = payload.osVersion,
                locale = payload.locale,
                uptimeMs = payload.uptimeMs,
                screenBrightness = payload.screenBrightness,
                remarks = payload.remarks,
                sosFlag = payload.sosFlag,
                status = payload.status,
                timestampIso = payload.timestamp,
                createdAt = System.currentTimeMillis()
            )
        }
    }
}