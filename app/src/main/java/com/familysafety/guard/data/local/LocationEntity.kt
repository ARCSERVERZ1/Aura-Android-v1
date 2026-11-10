package com.familysafety.guard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.familysafety.guard.data.model.LocationPayload

@Entity(tableName = "offline_location_queue")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val user: String,
    val placeName: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val visibility: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
) {
    fun toPayload(): LocationPayload {
        return LocationPayload(
            user = user,
            placeName = placeName,
            latitude = latitude,
            longitude = longitude,
            status = status,
            visibility = visibility
        )
    }

    companion object {
        fun fromPayload(payload: LocationPayload): LocationEntity {
            return LocationEntity(
                user = payload.user,
                placeName = payload.placeName,
                latitude = payload.latitude,
                longitude = payload.longitude,
                status = payload.status,
                visibility = payload.visibility,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}