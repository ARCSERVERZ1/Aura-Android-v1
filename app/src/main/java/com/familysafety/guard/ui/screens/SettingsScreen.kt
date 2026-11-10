package com.familysafety.guard.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val savedInterval by preferencesRepository.triggerIntervalFlow.collectAsState(initial = "15m")
    val reportBattery by preferencesRepository.reportBatteryStatusFlow.collectAsState(initial = true)
    val reportNetwork by preferencesRepository.reportNetworkStrengthFlow.collectAsState(initial = true)

    var baseUrlInput by remember(savedBaseUrl) { mutableStateOf(savedBaseUrl) }
    var apiUrlInput by remember(savedApiUrl) { mutableStateOf(savedApiUrl) }
    var selectedInterval by remember(savedInterval) { mutableStateOf(savedInterval) }

    val intervalOptions = listOf("Manual", "5m", "15m", "30m", "60m")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aura Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Section 1: Registered User Profile
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "REGISTERED IDENTIFIER",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = userIdentifier ?: "Not Registered",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Format: <Name> + <DDMMYYYY> persisted in DataStore",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Requirement 4: Location Trigger Interval / Manual Trigger
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "LOCATION TRIGGER INTERVAL",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        intervalOptions.forEach { option ->
                            FilterChip(
                                selected = selectedInterval == option,
                                onClick = {
                                    selectedInterval = option
                                    scope.launch {
                                        preferencesRepository.saveTriggerInterval(option)
                                    }
                                },
                                label = { Text(option) }
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                val success = safetyRepository.triggerLocationUpdate("ButtonTrigger")
                                Toast.makeText(
                                    context,
                                    if (success) "Location Sent to Server" else "Offline: Stored in Queue",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Location Now")
                    }
                }
            }

            // Requirement 4: Device Battery Status Reporting Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Device Battery Status Reporting",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "BroadcastReceiver triggers on <10% charge drop",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = reportBattery,
                        onCheckedChange = { isChecked ->
                            scope.launch {
                                preferencesRepository.setReportBatteryStatus(isChecked)
                            }
                        }
                    )
                }
            }

            // Requirement 4: Network Signal Strength Reporting Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Network Signal Strength Reporting",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "ConnectivityManager triggers when network drops",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = reportNetwork,
                        onCheckedChange = { isChecked ->
                            scope.launch {
                                preferencesRepository.setReportNetworkStrength(isChecked)
                            }
                        }
                    )
                }
            }

            // URL Configuration
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "NETWORK & ENDPOINTS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = baseUrlInput,
                        onValueChange = { baseUrlInput = it },
                        label = { Text("Base Web App URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = apiUrlInput,
                        onValueChange = { apiUrlInput = it },
                        label = { Text("Location API Endpoint") },
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
                        Text("Cached Requests:")
                        Text(
                            text = "$queueCount / 50 max",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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