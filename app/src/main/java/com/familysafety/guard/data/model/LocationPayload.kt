package com.familysafety.guard.data.model

import com.google.gson.annotations.SerializedName

/**
 * Exact Telemetry JSON Payload Structure as per specification:
 * {
 *     "username": "johndoe",
 *     "device_model": "Pixel 7",
 *     "latitude": 19.8762,
 *     "longitude": 75.3433,
 *     "is_charging": true,
 *     "battery_level": 84,
 *     "signal_strength": -75,
 *     "network_type": "WIFI",
 *     "os_version": "Android 14",
 *     "locale": "en_US",
 *     "uptime_ms": 345600000,
 *     "screen_brightness": 128,
 *     "remarks": "Battery Trigger at 25",
 *     "sos_flag": true,
 *     "status": 1,
 *     "timestamp": "2026-11-10T18:15:00Z"
 * }
 */
data class TelemetryPayload(
    @SerializedName("username")
    val username: String,

    @SerializedName("device_model")
    val deviceModel: String = android.os.Build.MODEL ?: "Pixel 7",

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("is_charging")
    val isCharging: Boolean = false,

    @SerializedName("battery_level")
    val batteryLevel: Int = 100,

    @SerializedName("signal_strength")
    val signalStrength: Int = -75,

    @SerializedName("network_type")
    val networkType: String = "WIFI",

    @SerializedName("os_version")
    val osVersion: String = "Android " + android.os.Build.VERSION.RELEASE,

    @SerializedName("locale")
    val locale: String = java.util.Locale.getDefault().toString(),

    @SerializedName("uptime_ms")
    val uptimeMs: Long = android.os.SystemClock.elapsedRealtime(),

    @SerializedName("screen_brightness")
    val screenBrightness: Int = 128,

    @SerializedName("remarks")
    val remarks: String = "Telemetry Update",

    @SerializedName("sos_flag")
    val sosFlag: Boolean = false,

    @SerializedName("status")
    val status: Int = 1,

    @SerializedName("timestamp")
    val timestamp: String // ISO-8601 string, e.g. "2026-11-10T18:15:00Z" (Never displayed visibly in UI)
)

typealias LocationPayload = TelemetryPayload