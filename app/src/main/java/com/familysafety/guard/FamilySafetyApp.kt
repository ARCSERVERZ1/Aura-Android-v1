package com.familysafety.guard

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.familysafety.guard.data.local.AppDatabase
import com.familysafety.guard.data.preferences.UserPreferencesRepository
import com.familysafety.guard.data.repository.LocationSafetyRepository
import com.familysafety.guard.network.NetworkConnectivityObserver
import com.familysafety.guard.receiver.BatteryLevelReceiver

class FamilySafetyApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var safetyRepository: LocationSafetyRepository
        private set

    lateinit var networkObserver: NetworkConnectivityObserver
        private set

    private val batteryReceiver = BatteryLevelReceiver()

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Initialize Local Room Database for max 50 request queue
        database = AppDatabase.getDatabase(this)

        // 2. Initialize DataStore Preferences
        preferencesRepository = UserPreferencesRepository(this)

        // 3. Initialize Location Safety Repository
        safetyRepository = LocationSafetyRepository(
            context = this,
            locationDao = database.locationDao(),
            preferencesRepository = preferencesRepository
        )

        // 4. Create Notification Channel for Emergency & Status alerts
        createNotificationChannel()

        // 5. Register Dynamic Battery Status Receiver (ACTION_BATTERY_CHANGED requires runtime registration)
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, batteryFilter)

        // 6. Register Network Observer for signal loss trigger & reconnect auto-flush
        networkObserver = NetworkConnectivityObserver(this, safetyRepository)
        networkObserver.startObserving()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_SAFETY,
                "Family Safety Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency triggers and location transmission status"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID_SAFETY = "family_safety_alerts_channel"
        lateinit var instance: FamilySafetyApp
            private set
    }
}