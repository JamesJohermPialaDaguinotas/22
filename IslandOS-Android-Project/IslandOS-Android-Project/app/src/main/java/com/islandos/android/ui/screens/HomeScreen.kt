package com.islandos.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islandos.android.data.IslandSettings
import com.islandos.android.model.DynamicIslandUiState
import com.islandos.android.model.LiveActivity
import com.islandos.android.system.PermissionHelper
import com.islandos.android.system.PermissionSnapshot
import com.islandos.android.ui.overlay.DynamicIslandOverlayContent
import com.islandos.android.ui.theme.IslandColors
import com.islandos.android.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    uiState: DynamicIslandUiState,
    settings: IslandSettings,
    permissions: PermissionSnapshot,
    viewModel: MainViewModel,
    onNavigateCustomize: () -> Unit,
    onNavigateFeatures: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IslandColors.OledBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "IslandOS",
            color = IslandColors.TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        // Live Dynamic Island Preview Stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0E121B), Color(0xFF07080C))
                    )
                )
                .border(1.dp, IslandColors.HairlineBorder, RoundedCornerShape(22.dp))
                .padding(top = 18.dp, start = 12.dp, end = 12.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            DynamicIslandOverlayContent(
                uiState = uiState.copy(isServiceEnabled = true),
                settings = settings,
                onTapIsland = { viewModel.coordinator.toggleExpanded() },
                onLongPressIsland = { viewModel.coordinator.cycleWidget() },
                onCollapse = { viewModel.coordinator.collapse() },
                onPlayPauseMusic = { viewModel.coordinator.mediaMonitor.togglePlayPause() },
                onNextMusic = { viewModel.coordinator.mediaMonitor.skipToNext() },
                onPreviousMusic = { viewModel.coordinator.mediaMonitor.skipToPrevious() },
                onSeekMusic = { viewModel.coordinator.mediaMonitor.seekTo(it) },
                onDismissNotification = { viewModel.coordinator.dismissNotification(it) },
                onOpenNotification = { viewModel.coordinator.openNotificationIntent(it) },
                onToggleTimer = { viewModel.coordinator.timerManager.togglePauseResume() },
                onAddTimerMinute = { viewModel.coordinator.timerManager.addOneMinute() },
                onStopTimer = { viewModel.coordinator.timerManager.stopAndClear() },
                onOpenCall = { viewModel.coordinator.callMonitor.openInCallScreen() }
            )

            Text(
                text = "Tap island to expand · Long-press to switch widgets",
                color = IslandColors.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )
        }

        // Master Enable / Disable Overlay Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(IslandColors.SurfaceDark)
                .border(1.dp, IslandColors.HairlineBorder, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "System-Wide Dynamic Island",
                    color = IslandColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (!permissions.overlayGranted) {
                        "Overlay permission required to draw over other apps"
                    } else if (settings.isEnabled) {
                        "Running across apps via foreground overlay service"
                    } else {
                        "Overlay service paused"
                    },
                    color = if (!permissions.overlayGranted) IslandColors.AccentAmber else IslandColors.TextSecondary,
                    fontSize = 12.5.sp
                )
            }
            if (!permissions.overlayGranted) {
                Button(
                    onClick = { context.startActivity(PermissionHelper.overlaySettingsIntent(context)) },
                    colors = ButtonDefaults.buttonColors(containerColor = IslandColors.AccentAmber, contentColor = Color.Black)
                ) {
                    Text("Grant", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Switch(
                    checked = settings.isEnabled,
                    onCheckedChange = { viewModel.setIslandEnabled(it) }
                )
            }
        }

        // Current Running Activity Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(IslandColors.SurfaceDark)
                .border(1.dp, IslandColors.HairlineBorder, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Current Running Activity",
                color = IslandColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            val description = when (val act = uiState.primaryActivity) {
                is LiveActivity.MusicActivity -> "Playing ${act.title} by ${act.artist} (${act.appName})"
                is LiveActivity.NotificationActivity -> "Notification from ${act.appName}: ${act.title}"
                is LiveActivity.TimerActivity -> "${act.label} running"
                is LiveActivity.CallActivity -> "Call: ${act.callerDisplay}"
                is LiveActivity.BatteryActivity -> "Battery ${act.levelPercent}% · ${act.chargingSource}"
                is LiveActivity.ConnectivityActivity -> "${act.wifiLabel} · Bluetooth ${if (act.bluetoothEnabled) "On" else "Off"}"
                null -> "Idle — play audio, plug in charger, or start a timer"
            }
            Text(
                text = description,
                color = IslandColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Active streams: ${uiState.allActivities.size} · Battery: ${uiState.battery?.levelPercent ?: "--"}%",
                color = IslandColors.TextSecondary,
                fontSize = 12.sp
            )
        }

        // Quick Feature Shortcuts
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(IslandColors.SurfaceDark)
                .border(1.dp, IslandColors.HairlineBorder, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick Shortcuts",
                color = IslandColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.startFocusTimer(5) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("5m Timer", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = { viewModel.startStopwatch() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Stopwatch", fontSize = 13.sp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateCustomize,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Customize Island", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onNavigateFeatures,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Manage Features", fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
