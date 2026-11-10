package com.familysafety.guard.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.familysafety.guard.data.local.LocationDao
import com.familysafety.guard.data.local.LocationEntity
import com.familysafety.guard.data.model.LocationPayload
import com.familysafety.guard.data.preferences.UserPreferencesRepository
import com.familysafety.guard.data.remote.LocationApiService
import com.familysafety.guard.service.LocationManagerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class LocationSafetyRepository(
    private val context: Context,
    private val locationDao: LocationDao,
    private val preferencesRepository: UserPreferencesRepository
) {

    private val apiService = LocationApiService.create()
    private val locationHelper = LocationManagerHelper(context)

    /**
     * Dispatches location with specified trigger status:
     * - "ButtonTrigger"
     * - "ChargeTrigger"
     * - "NetworkTrigger"
     */
    suspend fun triggerLocationUpdate(status: String): Boolean = withContext(Dispatchers.IO) {
        val userIdentifier = preferencesRepository.userIdentifierFlow.first() ?: "UnknownUser"
        val apiEndpoint = preferencesRepository.apiEndpointFlow.first()

        // 1. Fetch current or last known GPS location
        val location = locationHelper.getCurrentLocation()
        val lat = location?.latitude ?: 0.0
        val lng = location?.longitude ?: 0.0

        val payload = LocationPayload(
            user = userIdentifier,
            placeName = "Auto",
            latitude = lat,
            longitude = lng,
            status = status,
            visibility = 0
        )

        Log.d("LocationRepo", "Attempting trigger $status: $payload")

        // 2. Check current network connectivity
        val isOnline = isNetworkAvailable()

        if (isOnline) {
            try {
                val response = apiService.postLocation(apiEndpoint, payload)
                if (response.isSuccessful) {
                    Log.d("LocationRepo", "Direct POST successful: ${response.code()}")
                    // Also flush any queued requests now that network is confirmed working
                    flushOfflineQueue()
                    return@withContext true
                } else {
                    Log.w("LocationRepo", "Server returned ${response.code()}, saving to offline queue")
                    queuePayloadLocally(payload)
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e("LocationRepo", "Network call failed, caching offline: ${e.message}")
                queuePayloadLocally(payload)
                return@withContext false
            }
        } else {
            // Offline: Enforce Requirement - store up to 50 requests in local Room DB
            Log.d("LocationRepo", "Device offline. Caching request to local queue.")
            queuePayloadLocally(payload)
            return@withContext false
        }
    }

    private suspend fun queuePayloadLocally(payload: LocationPayload) {
        val entity = LocationEntity.fromPayload(payload)
        locationDao.insertWithQueueCap(entity, maxLimit = 50)
        val count = locationDao.getQueueCount()
        Log.d("LocationRepo", "Payload stored locally. Current queue count: $count / 50")
    }

    /**
     * Flushes up to 50 cached offline requests sequentially once internet is restored.
     */
    suspend fun flushOfflineQueue(): Int = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) return@withContext 0

        val apiEndpoint = preferencesRepository.apiEndpointFlow.first()
        val queuedItems = locationDao.getQueuedRequests()
        if (queuedItems.isEmpty()) return@withContext 0

        var sentCount = 0
        val sentIds = mutableListOf<Long>()

        for (item in queuedItems) {
            try {
                val response = apiService.postLocation(apiEndpoint, item.toPayload())
                if (response.isSuccessful) {
                    sentIds.add(item.id)
                    sentCount++
                } else {
                    // Stop on first failure to preserve order and avoid spamming server
                    break
                }
            } catch (e: Exception) {
                Log.e("LocationRepo", "Failed flushing queued item ${item.id}: ${e.message}")
                break
            }
        }

        if (sentIds.isNotEmpty()) {
            locationDao.deleteByIds(sentIds)
            Log.d("LocationRepo", "Successfully flushed $sentCount queued items.")
        }

        return@withContext sentCount
    }

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}