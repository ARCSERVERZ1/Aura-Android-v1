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
            val alertAlreadySent = prefs.batteryAlertSentFlow.first()

            if (batteryPct < 10.0f) {
                // Trigger payload POST EXACTLY ONCE when dropping below 10%
                if (!alertAlreadySent) {
                    Log.i("BatteryReceiver", "Battery dropped to $batteryPct% (< 10%). Firing ChargeTrigger!")
                    safetyRepo.triggerLocationUpdate(status = "ChargeTrigger")
                    // Set flag so it does not trigger again while remaining below 10%
                    prefs.setBatteryAlertSent(true)
                }
            } else if (batteryPct >= 15.0f) {
                // Reset flag once battery is charged back to safe level (hysteresis window >= 15%)
                if (alertAlreadySent) {
                    Log.d("BatteryReceiver", "Battery recovered to $batteryPct%. Resetting alert flag.")
                    prefs.setBatteryAlertSent(false)
                }
            }
        }
    }
}