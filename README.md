# Aura: Native Android Safety Application (Jetpack Compose)

This is a production-grade native Android application implementing the full Aura Architecture:

### 1. Persistent Background Execution
- **Foreground Service**: `AuraBackgroundSafetyService` runs continuously with an ongoing notification and partial WakeLock (`PowerManager.PARTIAL_WAKE_LOCK`), ensuring data fetching, SIM/battery monitoring, and payload transmissions are never killed by Android battery optimization / Doze mode.
- Auto-starts on device boot via `BootCompletedReceiver`.

### 2. Navigation & Gestures
- **System Back Button**: Handled via Compose `BackHandler` (navigates webview back or prevents leaving base URL).
- **No Swipe Gestures**: Drawer swipe gesture is explicitly disabled (`ModalNavigationDrawer(gesturesEnabled = false, ...)`).
- **Clean Footer**: Bottom navigation bar is completely removed for a full-screen viewport.

### 3. Minimal Top Header Bar
- **Left**: "Aura" brand title and shield icon.
- **Center**: Active server/API URL display badge.
- **Right**:
  - Refresh button: triggers an on-demand data fetch & payload transmission.
  - Hamburger menu icon: opens Settings drawer.

### 4. Settings Screen (Actual vs. Threshold)
- Displays **Actual vs. Threshold values side-by-side** for all monitored metrics:
  - Battery: Current actual level (%) vs. configured trigger threshold (%).
  - SIM 1 Signal: Current dBm vs. threshold (dBm).
  - SIM 2 Signal: Current dBm vs. threshold (dBm).
- Real-time trigger status badges and Persistent Service health card.
- "On-Demand Fetch" action button.

### 5. Features & Trigger Logic
- **Location Payload**: Full JSON payload with user identifier, location, battery, and structured `metrics_comparison` object.
- **Battery Drop Trigger**: Fires payload when battery falls below $X\%$ (Default 10%), and subsequent triggers fire on **every further battery percentage drop** below $X\%$. Resets when battery charges back to $\ge X\%$.
- **Dual SIM Monitoring**: Telephony signal strength observer for SIM 1 and SIM 2. Fires trigger **only once** when signal falls below threshold; resets trigger state **only after signal strength rises back above threshold** (single-fire hysteresis latch).
- **50-Item Offline Queue**: Persisted in Room database and automatically drained upon network reconnection.

## How to Build
- **GitHub Actions**: Push to GitHub to trigger `.github/workflows/build-apk.yml` and download the debug APK from the Artifacts tab.
- **Android Studio**: Open project root, sync Gradle (8.10.2 / AGP 8.7.2 / JDK 17), and run `assembleDebug`.
