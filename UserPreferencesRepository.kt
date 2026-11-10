package com.familysafety.guard.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_safety_preferences")

class UserPreferencesRepository(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_IDENTIFIER = stringPreferencesKey("user_identifier")
        val KEY_BASE_URL = stringPreferencesKey("base_url")
        val KEY_API_ENDPOINT = stringPreferencesKey("api_endpoint")
        val KEY_LAST_VISITED_URL = stringPreferencesKey("last_visited_url")
        val KEY_BATTERY_ALERT_SENT = booleanPreferencesKey("battery_alert_sent_under_10")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_TRIGGER_INTERVAL = stringPreferencesKey("trigger_interval")
        val KEY_REPORT_BATTERY = booleanPreferencesKey("report_battery_status")
        val KEY_REPORT_NETWORK = booleanPreferencesKey("report_network_strength")

        // New Feature: User-configurable Battery & Network Signal Thresholds
        val KEY_BATTERY_THRESHOLD = intPreferencesKey("battery_threshold_percent")
        val KEY_LAST_REPORTED_BATTERY_DROP = intPreferencesKey("last_reported_battery_drop")
        val KEY_NETWORK_SIGNAL_THRESHOLD = intPreferencesKey("network_signal_threshold_dbm")
        val KEY_NETWORK_TRIGGER_LATCHED = booleanPreferencesKey("network_trigger_latched")

        const val DEFAULT_BASE_URL = "http://192.168.1.18:9001"
        const val DEFAULT_API_ENDPOINT = "http://192.168.1.18:9001/appg/location/api/"
        const val DEFAULT_TRIGGER_INTERVAL = "15m"
        const val DEFAULT_BATTERY_THRESHOLD = 10 // 10%
        const val DEFAULT_NETWORK_THRESHOLD_DBM = -100 // -100 dBm
    }

    /**
     * Requirement: Concatenate input name with current setup date in DDMMYYYY format.
     * Example: Name: "Sanjay", Date: "10112026" -> Final Identifier: "Sanjay10112026".
     */
    fun generateUserIdentifier(rawName: String): String {
        val trimmed = rawName.trim()
        val dateFormat = SimpleDateFormat("ddMMyyyy", Locale.US)
        val currentDateFormatted = dateFormat.format(Date())
        return "$trimmed$currentDateFormatted"
    }

    val batteryThresholdFlow: Flow<Int> = dataStore.data
        .catch { emitEmptyPreferences(it) }
        .map { preferences -> preferences[KEY_BATTERY_THRESHOLD] ?: DEFAULT_BATTERY_THRESHOLD }

    val lastReportedBatteryDropFlow: Flow<Int?> = dataStore.data
        .catch { emitEmptyPreferences(it) }
        .map { preferences -> preferences[KEY_LAST_REPORTED_BATTERY_DROP] }

    val networkSignalThresholdFlow: Flow<Int> = dataStore.data
        .catch { emitEmptyPreferences(it) }
        .map { preferences -> preferences[KEY_NETWORK_SIGNAL_THRESHOLD] ?: DEFAULT_NETWORK_THRESHOLD_DBM }

    val networkTriggerLatchedFlow: Flow<Boolean> = dataStore.data
        .catch { emitEmptyPreferences(it) }
        .map { preferences -> preferences[KEY_NETWORK_TRIGGER_LATCHED] ?: false }

    val userIdentifierFlow: Flow<String?> = dataStore.data
        .catch { emitEmptyPreferences(it) }
        .map { preferences -> preferences[KEY_USER_IDENTIFIER] }

    val rawUserNameFlow: Flow<String?> = dataStore.data
        .catch { emitEmptyPreferences(it) }
        .map { preferences -> preferences[KEY_USER_NAME] }

    val baseUrlFlow: Flow<String> = dataStore.data
        .catch { emitEmptyPreferences(it) }
        .map { preferences -> preferences[KEY_BASE_URL] ?: DEFAULT_BASE_URL }

    val apiEndpointFlow: Flow<String> = dataStore.data
        .catch { emitEmptyPreferences(it) }
        .map { preferences -> preferences[KEY_API_ENDPOINT] ?: DEFAULT_API_ENDPOINT }

    val lastVisitedUrlFlow: Flow<String?> = dataStore.data
        .catch { emitEmptyPreferences(it) }
        .map { preferences -> preferences[KEY_LAST_VISITED_URL] }

    val isOnboardingCompletedFlow: Flow<Boolean> = dataStore.data
        .catch { emitEmptyPreferences(it) }
        .map { preferences -> preferences[KEY_ONBOARDING_COMPLETED] ?: false }

    val batteryAlertSentFlow: Flow<Boolean> = dataStore.data
        .catch { emitEmptyPreferences(it) }
        .map { preferences -> preferences[KEY_BATTERY_ALERT_SENT] ?: false }

    val triggerIntervalFlow: Flow<String> = dataStore.data
        .catch { emitEmptyPreferences(it) }
        .map { preferences -> preferences[KEY_TRIGGER_INTERVAL] ?: DEFAULT_TRIGGER_INTERVAL }

    val reportBatteryStatusFlow: Flow<Boolean> = dataStore.data
        .catch { emitEmptyPreferences(it) }
        .map { preferences -> preferences[KEY_REPORT_BATTERY] ?: true }

    val reportNetworkStrengthFlow: Flow<Boolean> = dataStore.data
        .catch { emitEmptyPreferences(it) }
        .map { preferences -> preferences[KEY_REPORT_NETWORK] ?: true }

    suspend fun saveUserProfile(rawName: String) {
        val identifier = generateUserIdentifier(rawName)
        dataStore.edit { preferences ->
            preferences[KEY_USER_NAME] = rawName.trim()
            preferences[KEY_USER_IDENTIFIER] = identifier
            preferences[KEY_ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun saveBaseUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[KEY_BASE_URL] = url.trim()
        }
    }

    suspend fun saveApiEndpoint(url: String) {
        dataStore.edit { preferences ->
            preferences[KEY_API_ENDPOINT] = url.trim()
        }
    }

    suspend fun saveLastVisitedUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_VISITED_URL] = url
        }
    }

    suspend fun setBatteryAlertSent(sent: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_BATTERY_ALERT_SENT] = sent
        }
    }

    suspend fun saveTriggerInterval(interval: String) {
        dataStore.edit { preferences ->
            preferences[KEY_TRIGGER_INTERVAL] = interval
        }
    }

    suspend fun setReportBatteryStatus(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_REPORT_BATTERY] = enabled
        }
    }

    suspend fun setReportNetworkStrength(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_REPORT_NETWORK] = enabled
        }
    }

    suspend fun saveBatteryThreshold(thresholdPercent: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_BATTERY_THRESHOLD] = thresholdPercent
        }
    }

    suspend fun setLastReportedBatteryDrop(level: Int?) {
        dataStore.edit { preferences ->
            if (level == null) {
                preferences.remove(KEY_LAST_REPORTED_BATTERY_DROP)
            } else {
                preferences[KEY_LAST_REPORTED_BATTERY_DROP] = level
            }
        }
    }

    suspend fun saveNetworkSignalThreshold(thresholdDbm: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_NETWORK_SIGNAL_THRESHOLD] = thresholdDbm
        }
    }

    suspend fun setNetworkTriggerLatched(latched: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NETWORK_TRIGGER_LATCHED] = latched
        }
    }

    suspend fun resetAllPreferences() {
        dataStore.edit { it.clear() }
    }

    private suspend fun emitEmptyPreferences(exception: Throwable) {
        if (exception is IOException) {
            // Log or handle
        } else {
            throw exception
        }
    }
}