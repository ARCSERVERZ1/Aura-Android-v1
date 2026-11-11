# Aura: Native Android Safety Application (Jetpack Compose)

This is a production-grade native Android application implementing the full Aura Architecture:

### 1. App Branding
- **Logo**: Iconic letter **A** in vibrant Orange Neon on pitch-black background.
- Vector adaptive launcher icons included (`ic_launcher.xml` & `ic_launcher_foreground.xml`).

### 2. Persistent Background Execution
- **Foreground Service**: `AuraBackgroundSafetyService` runs continuously with an ongoing notification and partial WakeLock (`PowerManager.PARTIAL_WAKE_LOCK`), ensuring data fetching, SIM/battery monitoring, and telemetry transmissions are never killed by Android OS battery optimization / Doze mode.
- Auto-starts on device boot via `BootCompletedReceiver`.

### 3. Exact 15-Field Telemetry Payload Schema
Dispatched to `<target_url>/appg/telemetry/api/`:
```json
{
    "username": "johndoe",
    "device_model": "Pixel 7",
    "latitude": 19.8762,
    "longitude": 75.3433,
    "is_charging": true,
    "battery_level": 84,
    "signal_strength": -75,
    "network_type": "WIFI",
    "os_version": "Android 14",
    "locale": "en_US",
    "uptime_ms": 345600000,
    "screen_brightness": 128,
    "remarks": "Battery Trigger at 25",
    "sos_flag": true,
    "status": 1,
    "timestamp": "2026-11-10T18:15:00Z"
}
```
*(Timestamp is included in the payload but strictly hidden from UI display).*

### 4. Exact Queue Removal Lifecycle
- Up to 50 payloads are buffered in Room Database when offline.
- When network connects or sync runs, items are POSTed to `/appg/telemetry/api/`.
- **Upon receiving an HTTP 200..299 response confirmation from the server**, the record is immediately deleted (`locationDao.deleteById(item.id)`).
- Failed calls remain in the queue for subsequent retry.

### 5. Web Authentication Persistence
- In `WebViewScreen`, `CookieManager` enables third-party cookies and persistent storage with `domStorageEnabled = true` and `databaseEnabled = true`.
- Calls `cookieManager.flush()` on page finished and application pause, securely persisting session tokens so the user is never prompted for password on app restarts.

### 6. General Settings & Endpoint Configuration
- **General Settings**: Clean view containing **only** the "Target URL" field.
- The telemetry endpoint dynamically appends `appg/telemetry/api/` to the configured base URL.

### 7. Battery & Dual SIM Monitoring with Continuous Sliders
- Continuous range sliders for:
  - Battery Threshold (1% to 100%) - triggers on drop below $X\%$ and every subsequent drop.
  - Signal Drop Threshold (-120 dBm to -60 dBm) - single-fire latch, resets on recovery.
- Live actual readings displayed side-by-side with threshold limits.

## How to Build
- **GitHub Actions**: Push to GitHub to trigger `.github/workflows/build-apk.yml` and download the debug APK from the Artifacts tab.
- **Android Studio**: Open project root, sync Gradle (8.10.2 / AGP 8.7.2 / JDK 17), and run `assembleDebug`.
