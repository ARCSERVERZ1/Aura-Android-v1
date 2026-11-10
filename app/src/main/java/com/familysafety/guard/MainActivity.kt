package com.familysafety.guard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.familysafety.guard.ui.screens.OnboardingScreen
import com.familysafety.guard.ui.screens.SettingsScreen
import com.familysafety.guard.ui.screens.WebViewScreen
import com.familysafety.guard.ui.theme.FamilySafetyTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else true

        if (!fineLocationGranted && !coarseLocationGranted) {
            Toast.makeText(
                this,
                "Location permission is critical for Family Safety emergency triggers.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()

        setContent {
            FamilySafetyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val app = application as FamilySafetyApp
                    val prefs = app.preferencesRepository
                    val isOnboardingCompleted by prefs.isOnboardingCompletedFlow
                        .collectAsState(initial = null)

                    if (isOnboardingCompleted != null) {
                        val startDestination = if (isOnboardingCompleted == true) "webview" else "onboarding"

                        NavHost(
                            navController = navController,
                            startDestination = startDestination
                        ) {
                            composable("onboarding") {
                                OnboardingScreen(
                                    preferencesRepository = prefs,
                                    onCompleted = {
                                        navController.navigate("webview") {
                                            popUpTo("onboarding") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("webview") {
                                WebViewScreen(
                                    safetyRepository = app.safetyRepository,
                                    preferencesRepository = prefs,
                                    onOpenSettings = {
                                        navController.navigate("settings")
                                    }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    preferencesRepository = prefs,
                                    safetyRepository = app.safetyRepository,
                                    database = app.database,
                                    onBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissionsLauncher.launch(missing.toTypedArray())
        }
    }
}