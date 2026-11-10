package com.familysafety.guard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import com.familysafety.guard.FamilySafetyApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Requirement 4.2:
 * Battery Low Trigger (status: "ChargeTrigger"):
 * - Register a BroadcastReceiver for battery state (Intent.ACTION_BATTERY_CHANGED).
 * - Trigger the payload POST exactly once when battery level drops below 10%.
 * - Use a state flag so it doesn't continuously trigger repeatedly while under 10%.
 */
class BatteryLevelReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BATTERY_CHANGED) return

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return

        val batteryPct = (level / scale.toFloat()) * 100f

        val app = context.applicationContext as? FamilySafetyApp ?: return
        val prefs = app.preferencesRepository
        val safetyRepo = app.safetyRepository

        CoroutineScope(Dispatchers.IO).launch {
            val threshold = prefs.batteryThresholdFlow.first()
            val lastReportedDrop = prefs.lastReportedBatteryDropFlow.first()
            val currentIntLevel = batteryPct.toInt()

            // Trigger when battery falls below X%, and fire on every further percentage drop below X%
            if (currentIntLevel < threshold) {
                if (lastReportedDrop == null || currentIntLevel < lastReportedDrop) {
                    Log.i("BatteryReceiver", "Battery dropped to $currentIntLevel% (< $threshold%). Firing ChargeTrigger!")
                    safetyRepo.triggerLocationUpdate(status = "ChargeTrigger")
                    // Track this level so next trigger only fires if it drops further (e.g. 9% -> 8% -> 7%)
                    prefs.setLastReportedBatteryDrop(currentIntLevel)
                }
            } else if (currentIntLevel >= threshold) {
                // Reset drop tracking when battery charges back to or above threshold
                if (lastReportedDrop != null) {
                    Log.d("BatteryReceiver", "Battery charged back to $currentIntLevel% (>= $threshold%). Resetting drop state.")
                    prefs.setLastReportedBatteryDrop(null)
                }
            }
        }
    }
}