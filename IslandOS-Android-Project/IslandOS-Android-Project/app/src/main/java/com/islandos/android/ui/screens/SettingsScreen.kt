package com.islandos.android.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islandos.android.BuildConfig
import com.islandos.android.data.IslandSettings
import com.islandos.android.system.PermissionHelper
import com.islandos.android.system.PermissionSnapshot
import com.islandos.android.ui.theme.IslandColors
import com.islandos.android.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    settings: IslandSettings,
    permissions: PermissionSnapshot,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshPermissions() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IslandColors.OledBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Settings",
            color = IslandColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        // System Permissions Section
        SectionContainer("System Permissions") {
            PermissionRow(
                title = "Display Over Other Apps",
                granted = permissions.overlayGranted,
                buttonLabel = "Configure",
                onClick = { context.startActivity(PermissionHelper.overlaySettingsIntent(context)) }
            )
            PermissionRow(
                title = "Notification Listener Access",
                granted = permissions.notificationListenerGranted,
                buttonLabel = "Configure",
                onClick = { context.startActivity(PermissionHelper.notificationListenerSettingsIntent()) }
            )
            PermissionRow(
                title = "Battery Optimization Unrestricted",
                granted = permissions.batteryOptimizationIgnored,
                buttonLabel = "Optimize",
                onClick = { context.startActivity(PermissionHelper.batteryOptimizationIntent(context)) }
            )
            PermissionRow(
                title = "Call State (READ_PHONE_STATE)",
                granted = permissions.phoneStateGranted,
                buttonLabel = if (permissions.phoneStateGranted) "Granted" else "Allow",
                onClick = { runtimePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PermissionRow(
                    title = "Bluetooth Connect (Android 12+)",
                    granted = permissions.bluetoothConnectGranted,
                    buttonLabel = if (permissions.bluetoothConnectGranted) "Granted" else "Allow",
                    onClick = { runtimePermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) }
                )
            }
        }

        // Startup & Behavior
        SectionContainer("Startup & Behavior") {
            SwitchRow(
                title = "Start on Device Boot",
                subtitle = "Restore overlay automatically via BootCompletedReceiver",
                checked = settings.startOnBoot,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(startOnBoot = v) } }
            )
            SwitchRow(
                title = "Hide in Landscape",
                subtitle = "Prevent overlap during full-screen games and video",
                checked = settings.hideInLandscape,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(hideInLandscape = v) } }
            )
            SwitchRow(
                title = "Auto-Hide When Idle",
                subtitle = "Hide pill completely when no live activity is active",
                checked = settings.autoHideWhenIdle,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(autoHideWhenIdle = v) } }
            )
            SwitchRow(
                title = "Haptic Feedback",
                subtitle = "Tactile response on island tap and long-press",
                checked = settings.hapticFeedbackEnabled,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(hapticFeedbackEnabled = v) } }
            )
        }

        // Privacy & Lock Screen
        SectionContainer("Privacy & Lock Screen") {
            SwitchRow(
                title = "Show on Lock Screen",
                subtitle = "Depends on OEM Keyguard policy (Pixel & AOSP supported; some OEMs restrict overlays)",
                checked = settings.showOnLockScreen,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(showOnLockScreen = v) } }
            )
            SwitchRow(
                title = "Redact Sensitive Notification Text",
                subtitle = "Hide message body when device is locked",
                checked = settings.privacyRedactNotifications,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(privacyRedactNotifications = v) } }
            )
        }

        // Battery Optimization Guidance
        SectionContainer("OEM Background Execution Notes") {
            Text(
                text = "Xiaomi HyperOS / MIUI, Samsung One UI, and OnePlus OxygenOS may pause background services after reboot or idle sleep. Lock IslandOS in Recents and set Battery mode to Unrestricted in App Info.",
                color = IslandColors.TextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )
            OutlinedButton(
                onClick = { context.startActivity(PermissionHelper.appDetailsSettingsIntent(context)) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Open System App Info", fontSize = 13.sp)
            }
        }

        // About Application
        SectionContainer("About IslandOS") {
            Text("Version ${BuildConfig.VERSION_NAME} · Android SDK ${Build.VERSION.SDK_INT}", color = IslandColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                text = "Built with Kotlin, Jetpack Compose, Material 3, WindowManager Overlay, NotificationListenerService, MediaSessionManager, and DataStore Preferences.",
                color = IslandColors.TextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SectionContainer(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(IslandColors.SurfaceDark)
            .border(1.dp, IslandColors.HairlineBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, color = IslandColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = IslandColors.TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            Text(
                text = if (granted) "Granted" else "Not granted",
                color = if (granted) IslandColors.AccentEmerald else IslandColors.AccentAmber,
                fontSize = 12.sp
            )
        }
        OutlinedButton(onClick = onClick, shape = RoundedCornerShape(10.dp)) {
            Text(buttonLabel, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = IslandColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = IslandColors.TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
