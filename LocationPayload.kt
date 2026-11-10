package com.familysafety.guard.data.model

import com.google.gson.annotations.SerializedName

/**
 * Core JSON Payload Schema:
 * {
 *   "user": "<Stored User Name + DDMMYYYY>",
 *   "place_name": "Auto",
 *   "latitude": <Float/Double>,
 *   "longitude": <Float/Double>,
 *   "status": "<TRIGGER_TYPE>",
 *   "visibility": 0
 * }
 */
data class LocationPayload(
    @SerializedName("user")
    val user: String,

    @SerializedName("place_name")
    val placeName: String = "Auto",

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("status")
    val status: String, // "ButtonTrigger", "ChargeTrigger", "NetworkTrigger", or "ManualSync"

    @SerializedName("visibility")
    val visibility: Int = 0
)