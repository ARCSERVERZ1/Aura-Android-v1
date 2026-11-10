package com.familysafety.guard.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.familysafety.guard.data.preferences.UserPreferencesRepository
import com.familysafety.guard.data.repository.LocationSafetyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    safetyRepository: LocationSafetyRepository,
    preferencesRepository: UserPreferencesRepository,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    val baseUrl by preferencesRepository.baseUrlFlow.collectAsState(initial = "http://192.168.1.18:9001")
    val lastVisitedUrl by preferencesRepository.lastVisitedUrlFlow.collectAsState(initial = null)
    val userIdentifier by preferencesRepository.userIdentifierFlow.collectAsState(initial = "")

    /**
     * Requirement 2: System/hardware back-button override logic:
     * - Check if current WebView state allows backward navigation (webView.canGoBack()).
     * - IF canGoBack() is TRUE and current URL != BASE_URL: Navigate back (webView.goBack()).
     * - IF current URL is equal to BASE_URL (or canGoBack() is FALSE): Block navigation and display notification:
     *   "Cannot go back, it is the base URL"
     */
    BackHandler {
        val current = webViewInstance?.url ?: ""
        val isBaseUrl = current == baseUrl || current == "$baseUrl/" || current.isEmpty()

        if (webViewInstance?.canGoBack() == true && !isBaseUrl) {
            webViewInstance?.goBack()
        } else {
            Toast.makeText(context, "Cannot go back, it is the base URL", Toast.LENGTH_SHORT).show()
        }
    }

    // Disable swipe gestures: Drawer opens ONLY when clicking the hamburger menu icon
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Aura",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Text(
                            text = userIdentifier ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Web Application Home") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            webViewInstance?.loadUrl(baseUrl)
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Settings entry inside navigation menu
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onOpenSettings()
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    label = { Text("On-Demand Fetch") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            val success = safetyRepository.triggerLocationUpdate("OnDemandFetch")
                            Toast.makeText(
                                context,
                                if (success) "On-Demand Payload Sent" else "Saved to Local Queue",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Top Header Bar (Minimal Design):
                // Left: App Title / Brand logo
                // Center: Display the server/API URL being used
                // Right: Refresh button (triggers on-demand fetch) & Hamburger menu icon
                TopAppBar(
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = baseUrl.removePrefix("http://").removePrefix("https://"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    },
                    navigationIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Aura Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Aura",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        // Right: Refresh button (triggers on-demand data fetch)
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val success = safetyRepository.triggerLocationUpdate("OnDemandFetch")
                                    webViewInstance?.reload()
                                    Toast.makeText(
                                        context,
                                        if (success) "On-Demand Data Fetched" else "Offline: Stored in Queue",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "On-Demand Fetch Refresh",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Right: Hamburger menu icon (opens app settings drawer/screen)
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Settings Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
            // Clean Footer: Bottom navigation / bottom bar must be completely empty/removed
        ) { innerPadding ->
            // Requirement 2: Full-screen Webview component occupying the main layout viewport
            // Requirement 3: Live Radars, Safe Zones, and Contacts UI completely removed
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    url?.let {
                                        currentUrl = it
                                        scope.launch {
                                            preferencesRepository.saveLastVisitedUrl(it)
                                        }
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    url?.let { currentUrl = it }
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    return false // Stay inside embedded WebView
                                }
                            }

                            webViewInstance = this

                            // Set webview src directly to target base URL (restoring last visited if available)
                            val targetUrl = lastVisitedUrl ?: baseUrl
                            loadUrl(targetUrl)
                        }
                    },
                    update = { view ->
                        webViewInstance = view
                    }
                )

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}