package com.islandos.android.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islandos.android.data.IslandSettings
import com.islandos.android.model.DynamicIslandUiState
import com.islandos.android.ui.theme.IslandColors
import com.islandos.android.ui.viewmodel.MainViewModel

@Composable
fun FeaturesScreen(
    uiState: DynamicIslandUiState,
    settings: IslandSettings,
    viewModel: MainViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IslandColors.OledBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Live Features",
            color = IslandColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        FeatureToggleCard(
            title = "Music Player",
            subtitle = "MediaSessionManager integration with album artwork, seekable progress bar, play/pause, and track skipping.",
            checked = settings.musicEnabled,
            onCheckedChange = { enabled -> viewModel.updateSettings { it.copy(musicEnabled = enabled) } }
        )

        FeatureToggleCard(
            title = "Notifications",
            subtitle = "NotificationListenerService alerts with app icons, multi-notification queue, and direct dismissal.",
            checked = settings.notificationsEnabled,
            onCheckedChange = { enabled -> viewModel.updateSettings { it.copy(notificationsEnabled = enabled) } }
        )

        FeatureToggleCard(
            title = "Battery & Charging",
            subtitle = "Real-time BroadcastReceiver tracking charge level, AC/USB/Wireless source, voltage, and thermal status.",
            checked = settings.batteryEnabled,
            onCheckedChange = { enabled -> viewModel.updateSettings { it.copy(batteryEnabled = enabled) } }
        )

        FeatureToggleCard(
            title = "Timer & Stopwatch",
            subtitle = "Drift-free countdown timer and stopwatch with circular progress ring right on the island.",
            checked = settings.timerEnabled,
            onCheckedChange = { enabled -> viewModel.updateSettings { it.copy(timerEnabled = enabled) } }
        )

        if (settings.timerEnabled) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(IslandColors.SurfaceElevated)
                    .border(1.dp, IslandColors.HairlineBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Timer & Stopwatch Controls", color = IslandColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.startFocusTimer(1) }, modifier = Modifier.weight(1f)) {
                        Text("1m", fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { viewModel.startFocusTimer(5) }, modifier = Modifier.weight(1f)) {
                        Text("5m", fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { viewModel.startFocusTimer(25) }, modifier = Modifier.weight(1f)) {
                        Text("25m", fontSize = 12.sp)
                    }
                    Button(onClick = { viewModel.startStopwatch() }, modifier = Modifier.weight(1.3f)) {
                        Text("Stopwatch", fontSize = 12.sp)
                    }
                }
            }
        }

        FeatureToggleCard(
            title = "Connectivity",
            subtitle = "Live Wi-Fi NetworkCallback, Bluetooth adapter/audio profile connections, and Airplane mode indicator.",
            checked = settings.connectivityEnabled,
            onCheckedChange = { enabled -> viewModel.updateSettings { it.copy(connectivityEnabled = enabled) } }
        )

        FeatureToggleCard(
            title = "Incoming Calls",
            subtitle = "TelephonyCallback (API 31+) & CATEGORY_CALL notification enrichment. Opens the system dialer securely without replacing your phone app.",
            checked = settings.callsEnabled,
            onCheckedChange = { enabled -> viewModel.updateSettings { it.copy(callsEnabled = enabled) } }
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun FeatureToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(IslandColors.SurfaceDark)
            .border(1.dp, IslandColors.HairlineBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = IslandColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = IslandColors.TextSecondary, fontSize = 12.5.sp, lineHeight = 18.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
