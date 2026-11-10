package com.familysafety.guard.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familysafety.guard.data.local.AppDatabase
import com.familysafety.guard.data.preferences.UserPreferencesRepository
import com.familysafety.guard.data.repository.LocationSafetyRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesRepository: UserPreferencesRepository,
    safetyRepository: LocationSafetyRepository,
    database: AppDatabase,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val userIdentifier by preferencesRepository.userIdentifierFlow.collectAsState(initial = "")
    val savedBaseUrl by preferencesRepository.baseUrlFlow.collectAsState(initial = "http://192.168.1.18:9001")
    val savedApiUrl by preferencesRepository.apiEndpointFlow.collectAsState(initial = "http://192.168.1.18:9001/appg/location/api/")
    val queueCount by database.locationDao().observeQueueCount().collectAsState(initial = 0)
    
    // Configurable Thresholds
    val batteryThreshold by preferencesRepository.batteryThresholdFlow.collectAsState(initial = 10)
    val networkThresholdDbm by preferencesRepository.networkSignalThresholdFlow.collectAsState(initial = -100)
    val lastBatteryDrop by preferencesRepository.lastReportedBatteryDropFlow.collectAsState(initial = null)
    val isNetworkLatched by preferencesRepository.networkTriggerLatchedFlow.collectAsState(initial = false)

    var baseUrlInput by remember(savedBaseUrl) { mutableStateOf(savedBaseUrl) }
    var apiUrlInput by remember(savedApiUrl) { mutableStateOf(savedApiUrl) }
    var isSendingOnDemand by remember { mutableStateOf(false) }

    // Live / Simulated actual readings
    val actualBattery = 85
    val actualSim1Dbm = -85
    val actualSim2Dbm = -105

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aura Settings & Thresholds") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            isSendingOnDemand = true
                            val success = safetyRepository.triggerLocationUpdate("OnDemandFetch")
                            isSendingOnDemand = false
                            Toast.makeText(context, if (success) "On-Demand Data Transmitted" else "Stored in Offline Queue", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "On-Demand Refresh", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile & Identifier
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ACTIVE USER IDENTIFIER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(userIdentifier ?: "Not Registered", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text("DataStore", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Architecture Section: Persistent Background Execution Service
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PERSISTENT BACKGROUND SERVICE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF10B981).copy(alpha = 0.15f)) {
                            Text("RUNNING", color = Color(0xFF10B981), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "AuraForegroundService executes continuously in background with persistent notification and partial WakeLock to prevent OS battery optimization termination.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Side-by-Side Actual vs. Threshold Displays
            Text("ACTUAL VS. THRESHOLD COMPARISON", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            // Metric 1: Battery Level Trigger (Repeating on drop below threshold)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("1. BATTERY LEVEL MONITORING", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("Fires on <X% & every drop", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Side-by-Side Values
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Actual Reading
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("ACTUAL READING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("$actualBattery", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = if (actualBattery < batteryThreshold) Color(0xFFEF4444) else Color(0xFF10B981))
                                    Text("%", fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp, start = 2.dp))
                                }
                                Text("Intent.ACTION_BATTERY", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Threshold Limit
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("SET THRESHOLD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("<$batteryThreshold", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("%", fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp, start = 2.dp))
                                }
                                Text(if (lastBatteryDrop != null) "Last fired: $lastBatteryDrop%" else "Status: Armed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // User Configurable Threshold Chips
                    Text("Configure Battery Threshold Limit:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5, 10, 15, 20, 25).forEach { pct ->
                            FilterChip(
                                selected = batteryThreshold == pct,
                                onClick = {
                                    scope.launch { preferencesRepository.saveBatteryThreshold(pct) }
                                },
                                label = { Text("$pct%") }
                            )
                        }
                    }
                }
            }

            // Metric 2: Network Monitoring (SIM 1 / SIM 2)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("2. NETWORK MONITORING (DUAL SIM)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isNetworkLatched) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                if (isNetworkLatched) "TRIGGER LATCHED" else "ARMED (SINGLE-FIRE)",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isNetworkLatched) Color(0xFFEF4444) else Color(0xFF10B981)
                            )
                        }
                    }

                    // Side-by-Side Actual Readings (SIM 1 & SIM 2) vs Threshold
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // SIM 1 Actual
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("SIM 1 ACTUAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$actualSim1Dbm dBm", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (actualSim1Dbm < networkThresholdDbm) Color(0xFFEF4444) else Color(0xFF10B981))
                                Text("Good (4G/LTE)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // SIM 2 Actual
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("SIM 2 ACTUAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$actualSim2Dbm dBm", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (actualSim2Dbm < networkThresholdDbm) Color(0xFFEF4444) else Color(0xFF10B981))
                                Text("Poor (Low)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Network Threshold Setting
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Signal Drop Threshold:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("$networkThresholdDbm dBm", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(-115, -105, -100, -95, -85).forEach { dbm ->
                            FilterChip(
                                selected = networkThresholdDbm == dbm,
                                onClick = {
                                    scope.launch { preferencesRepository.saveNetworkSignalThreshold(dbm) }
                                },
                                label = { Text("$dbm dBm") }
                            )
                        }
                    }
                    Text(
                        "Trigger fires once when signal falls below threshold; resets only when signal rises back above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // On-Demand Fetch Action Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("3. ON-DEMAND FETCH", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Immediately gathers current GPS coordinates, actual SIM readings, and battery telemetry, and dispatches the payload to the server.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    
                    Button(
                        onClick = {
                            scope.launch {
                                isSendingOnDemand = true
                                val success = safetyRepository.triggerLocationUpdate("OnDemandFetch")
                                isSendingOnDemand = false
                                Toast.makeText(context, if (success) "On-Demand Payload Sent" else "Saved to Local Queue", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSendingOnDemand) "Transmitting..." else "Trigger On-Demand Fetch Now")
                    }
                }
            }

            // Section 4: Target Endpoints Configuration
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("TARGET URL & API ENDPOINT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = baseUrlInput,
                        onValueChange = { baseUrlInput = it },
                        label = { Text("Base Web App URL (BASE_URL)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = apiUrlInput,
                        onValueChange = { apiUrlInput = it },
                        label = { Text("Location POST Endpoint (/appg/location/api/)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                preferencesRepository.saveBaseUrl(baseUrlInput)
                                preferencesRepository.saveApiEndpoint(apiUrlInput)
                                Toast.makeText(context, "Configurations Saved", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Configurations")
                    }
                }
            }

            // Offline 50-Request Buffer
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Offline Buffer Queue:")
                        Text(
                            text = "$queueCount / 50 max",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    val sent = safetyRepository.flushOfflineQueue()
                                    Toast.makeText(context, "Flushed $sent items", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Flush Queue")
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    database.locationDao().clearAll()
                                    Toast.makeText(context, "Queue cleared", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
        }
    }
}