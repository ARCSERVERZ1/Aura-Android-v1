package com.familysafety.guard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.familysafety.guard.FamilySafetyApp
import com.familysafety.guard.MainActivity
import com.familysafety.guard.telephony.SimNetworkObserver
import kotlinx.coroutines.*

/**
 * Key System Architecture: Persistent Background Execution
 * Continuous background Foreground Service exempt from battery optimization,
 * with ongoing notification, partial WakeLock, and SIM/telemetry monitoring.
 */
class AuraBackgroundSafetyService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var wakeLock: PowerManager.WakeLock? = null
    private var simObserver: SimNetworkObserver? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "aura_safety_persistent_channel"
        const val ACTION_ON_DEMAND_FETCH = "com.familysafety.guard.ACTION_ON_DEMAND_FETCH"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("AuraBgService", "Aura Background Safety Service created.")

        createNotificationChannel()
        acquireWakeLock()

        val app = application as FamilySafetyApp
        simObserver = SimNetworkObserver(this, app.safetyRepository, app.preferencesRepository)

        val notification = buildPersistentNotification(
            "Aura Safety Active",
            "Continuous background monitoring & location triggers enabled"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Start background polling loop
        startContinuousMonitoringLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_ON_DEMAND_FETCH) {
            triggerOnDemandFetch()
        }
        return START_STICKY
    }

    private fun startContinuousMonitoringLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    simObserver?.checkSignalAndEvaluateTrigger()
                } catch (e: Exception) {
                    Log.e("AuraBgService", "Background monitoring error: ${e.message}")
                }
                delay(15000) // Poll active SIM readings every 15 seconds
            }
        }
    }

    private fun triggerOnDemandFetch() {
        serviceScope.launch {
            val app = application as FamilySafetyApp
            Log.i("AuraBgService", "Executing On-Demand Fetch from background service")
            app.safetyRepository.triggerLocationUpdate(status = "OnDemandFetch")
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Aura:BackgroundSafetyWakeLock").apply {
                setReferenceCounted(false)
                acquire(24 * 60 * 60 * 1000L) // 24 hours safeguard
            }
        } catch (e: Exception) {
            Log.w("AuraBgService", "Could not acquire WakeLock: ${e.message}")
        }
    }

    private fun buildPersistentNotification(title: String, text: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aura Persistent Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Guarantees continuous safety monitoring in background"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        Log.i("AuraBgService", "Aura Background Safety Service destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}