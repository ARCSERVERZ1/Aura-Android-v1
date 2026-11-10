package com.familysafety.guard.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.familysafety.guard.data.repository.LocationSafetyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Requirement 4.3:
 * Signal Lost Trigger (status: "NetworkTrigger"):
 * - Monitor network connectivity using ConnectivityManager.NetworkCallback.
 * - Trigger payload POST just before/as connectivity drops to disconnected state,
 *   capturing the last known location.
 * - When network is restored, automatically flush the offline 50-request queue!
 */
class NetworkConnectivityObserver(
    private val context: Context,
    private val safetyRepository: LocationSafetyRepository
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var wasConnected = true

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d("NetworkObserver", "Network restored (Available). Triggering offline queue sync.")
            wasConnected = true

            // When internet is back, flush up to 50 stored offline requests
            CoroutineScope(Dispatchers.IO).launch {
                val flushedCount = safetyRepository.flushOfflineQueue()
                if (flushedCount > 0) {
                    Log.i("NetworkObserver", "Auto-flushed $flushedCount offline requests!")
                }
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Log.w("NetworkObserver", "Network lost! Firing NetworkTrigger.")

            if (wasConnected) {
                wasConnected = false
                // Trigger payload POST as connectivity drops
                CoroutineScope(Dispatchers.IO).launch {
                    safetyRepository.triggerLocationUpdate(status = "NetworkTrigger")
                }
            }
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, capabilities)
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            if (!hasInternet && wasConnected) {
                wasConnected = false
                CoroutineScope(Dispatchers.IO).launch {
                    safetyRepository.triggerLocationUpdate(status = "NetworkTrigger")
                }
            }
        }
    }

    fun startObserving() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    fun stopObserving() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Already unregistered
        }
    }
}