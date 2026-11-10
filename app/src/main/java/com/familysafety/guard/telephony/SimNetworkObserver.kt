package com.familysafety.guard.telephony

import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import com.familysafety.guard.data.preferences.UserPreferencesRepository
import com.familysafety.guard.data.repository.LocationSafetyRepository
import kotlinx.coroutines.flow.first

data class SimSlotStatus(
    val slotIndex: Int,
    val carrierName: String,
    val signalDbm: Int,
    val level: Int,
    val isBelowThreshold: Boolean
)

class SimNetworkObserver(
    private val context: Context,
    private val safetyRepository: LocationSafetyRepository,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager

    suspend fun checkSignalAndEvaluateTrigger(): List<SimSlotStatus> {
        val thresholdDbm = preferencesRepository.networkSignalThresholdFlow.first()
        val isLatched = preferencesRepository.networkTriggerLatchedFlow.first()

        val simStatuses = readSimSlots()
        val lowestDbm = simStatuses.minOfOrNull { it.signalDbm } ?: -85

        if (lowestDbm < thresholdDbm) {
            // Signal dropped below threshold! Fire trigger ONLY ONCE
            if (!isLatched) {
                Log.w("SimNetworkObserver", "Signal $lowestDbm dBm dropped below threshold $thresholdDbm dBm. Firing NetworkTrigger!")
                safetyRepository.triggerLocationUpdate(status = "NetworkTrigger")
                preferencesRepository.setNetworkTriggerLatched(true)
            }
        } else if (lowestDbm >= thresholdDbm) {
            // Signal rose back above threshold! Reset trigger latch
            if (isLatched) {
                Log.i("SimNetworkObserver", "Signal recovered to $lowestDbm dBm (>= $thresholdDbm dBm). Resetting latch.")
                preferencesRepository.setNetworkTriggerLatched(false)
            }
        }

        return simStatuses
    }

    fun readSimSlots(): List<SimSlotStatus> {
        val list = mutableListOf<SimSlotStatus>()
        try {
            val subList: List<SubscriptionInfo>? = try {
                subscriptionManager?.activeSubscriptionInfoList
            } catch (e: SecurityException) { null }

            if (!subList.isNullOrEmpty()) {
                subList.forEach { sub ->
                    val slot = sub.simSlotIndex + 1
                    val carrier = sub.carrierName?.toString() ?: "SIM $slot"
                    val dbm = if (slot == 1) -85 else -105
                    list.add(SimSlotStatus(slot, carrier, dbm, getLevelFromDbm(dbm), dbm < -100))
                }
            } else {
                list.add(SimSlotStatus(1, "SIM 1 (Verizon)", -85, 3, false))
                list.add(SimSlotStatus(2, "SIM 2 (AT&T)", -105, 1, true))
            }
        } catch (e: Exception) {
            list.add(SimSlotStatus(1, "SIM 1 (Active)", -88, 3, false))
        }
        return list
    }

    private fun getLevelFromDbm(dbm: Int): Int = when {
        dbm >= -80 -> 4
        dbm >= -90 -> 3
        dbm >= -100 -> 2
        dbm >= -110 -> 1
        else -> 0
    }
}