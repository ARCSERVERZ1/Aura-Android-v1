# Aura: Native Android Webview Application (Jetpack Compose)

This is a production-grade native Android application implementing the Aura Specification:
1. **App Rebranding & Header Layout**:
   - Header title: "Aura".
   - Top action: "Send Location Type" icon button that immediately dispatches location update payload to the backend server.
2. **Main WebView Container & Navigation Rules**:
   - Full-screen Webview occupying the main layout viewport.
   - Set directly to target `BASE_URL` ("http://192.168.1.18:9001").
   - Back-button override logic:
     - Checks if `webView.canGoBack()`. If true and not at Base URL, navigates back (`webView.goBack()`).
     - If current URL == `BASE_URL` or cannot go back, blocks navigation and displays:
       `"Cannot go back, it is the base URL"`.
3. **Cleaned View Hierarchy**:
   - Dedicated full-screen WebView with legacy Radars, Safe Zones, and Contacts UI completely removed.
4. **Navigation Drawer & Settings**:
   - Side-navigation drawer accessible via Hamburger Menu.
   - Settings Screen with:
     - Location trigger interval configuration (Manual, 5m, 15m, 30m, 60m) & "Send Location Now" button.
     - Device battery status reporting toggle (<10% trigger).
     - Network signal strength reporting toggle (disconnect trigger).
     - Target Base URL and Location API Endpoint configuration.
5. **Offline Queue (50 Requests Max)**:
   - Persisted in local Room Database when disconnected, automatically synced upon reconnect.

## How to Run in Android Studio
1. Open Android Studio (Ladybug / Koala / Hedgehog or newer).
2. Click "Open..." and select this project directory.
3. Allow Gradle to sync.
4. Run on an Android Emulator or Physical Device (API 26+).
