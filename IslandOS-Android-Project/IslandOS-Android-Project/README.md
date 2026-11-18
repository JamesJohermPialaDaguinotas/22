# IslandOS — Native Android Dynamic Island

A complete Android Studio project implementing a system-wide Dynamic Island overlay using **Kotlin**, **Jetpack Compose**, **Material 3**, **WindowManager (`TYPE_APPLICATION_OVERLAY`)**, **NotificationListenerService**, **MediaSessionManager**, **TelephonyCallback**, **BroadcastReceivers**, **Foreground Services**, and **DataStore Preferences**.

## Project Architecture

- `app/src/main/java/com/islandos/android/DynamicIslandApp.kt` — Application class creating the foreground notification channel and singleton `DynamicIslandStateCoordinator`.
- `data/SettingsRepository.kt` — Persistent Jetpack DataStore repository storing geometry, Liquid Glass shader toggle, theme presets, feature switches, and privacy preferences.
- `model/IslandModels.kt` — Domain models for Music, Notification, Battery, Timer/Stopwatch, Incoming Call, and Connectivity activities.
- `system/` — Real Android OS monitors (`MediaMonitor`, `BatteryMonitor`, `ConnectivityMonitor`, `CallMonitor`, `TimerManager`, `PermissionHelper`).
- `service/` — `DynamicIslandOverlayService` (`LifecycleService` + `SavedStateRegistryOwner` + `ComposeView` inside `WindowManager`), `IslandNotificationListenerService`, `DynamicIslandStateCoordinator`, and `BootCompletedReceiver`.
- `ui/` — `DynamicIslandOverlayContent` (spring morphing + Liquid Glass), `OnboardingScreen`, `HomeScreen`, `CustomizationScreen`, `FeaturesScreen`, and `SettingsScreen`.

## Build APK on GitHub (No Android Studio Required)

This repository includes `.github/workflows/build-apk.yml` so GitHub Actions compiles your Android APKs in the cloud automatically:

1. Push (or upload) all files in this folder — including `.github/workflows/build-apk.yml` — to a GitHub repository on the `main` branch.
2. Open the **Actions** tab in your GitHub repository and select **Build IslandOS Android APK** (triggers automatically on push, or click **Run workflow**).
3. Once finished, download **IslandOS-debug-apk** from **Artifacts**, unzip it, and install `app-debug.apk` directly on your Android phone.
4. *(Optional Signed Release)* Add repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` under **Settings → Secrets and variables → Actions** to automatically sign `IslandOS-release-apk`.

## Local Android Studio Installation (Optional)

1. Extract or open `android-project/` in **Android Studio Ladybug (2024.2.1)** or newer.
2. Ensure **JDK 17** and **Android SDK Platform 35** are installed via SDK Manager.
3. Let Gradle sync (`gradle-8.9`).
4. Connect a physical Android device (Android 8.0 / API 26+, recommended Android 12–15) with **USB Debugging** enabled.
5. Click **Run `app`** or run from terminal:
   ```bash
   ./gradlew installDebug
   ```

## Debugging & Testing Instructions

- **Verify Overlay Window Bounds & Touch Passthrough**:
  ```bash
  adb shell dumpsys window windows | grep -i islandos
  ```
- **Verify NotificationListenerService Binding**:
  ```bash
  adb shell dumpsys notification --noredact | grep -i IslandNotificationListenerService
  ```
- **Inspect Live Service Logs**:
  ```bash
  adb logcat -s DynamicIslandOverlayService IslandNotificationListener MediaMonitor
  ```
- **Test Boot Restoration Without Rebooting**:
  ```bash
  adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -p com.islandos.android
  ```

## Android Version & OEM Limitations

- **Lock Screen Overlay**: Standard AOSP / Pixel devices permit `TYPE_APPLICATION_OVERLAY` over the unlocked shade and certain lock states, while Samsung One UI, Xiaomi HyperOS, and Android 14+ secure lockscreens may hide non-system overlays for security.
- **Incoming Call Actions**: Android restricts silent call answering/ending to the default `InCallService` dialer. IslandOS legitimately detects call states via `TelephonyCallback` (when `READ_PHONE_STATE` is granted) + `Notification.CATEGORY_CALL` and opens the active system Dialer screen via `TelecomManager.showInCallScreen()` or the notification's `PendingIntent`.
- **Liquid Glass Blur**: Hardware-accelerated `RenderEffect.createBlurEffect` requires Android 12 (API 31)+; older devices gracefully fall back to translucent alpha layers with specular borders.

## Building a Signed Release APK

1. Generate an upload keystore (if you do not already have one):
   ```bash
   keytool -genkey -v -keystore islandos-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias islandos
   ```
2. Configure signing in `app/build.gradle.kts` (`signingConfigs { create("release") { ... } }`) or via **Build -> Generate Signed Bundle / APK** in Android Studio.
3. Build the minified release APK:
   ```bash
   ./gradlew assembleRelease
   ```
   Output APK location: `app/build/outputs/apk/release/app-release.apk`.
