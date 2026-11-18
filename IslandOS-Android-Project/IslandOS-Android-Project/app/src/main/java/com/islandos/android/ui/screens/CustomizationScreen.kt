package com.islandos.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islandos.android.data.IslandSettings
import com.islandos.android.model.DynamicIslandUiState
import com.islandos.android.model.IslandTheme
import com.islandos.android.ui.overlay.DynamicIslandOverlayContent
import com.islandos.android.ui.theme.IslandColors
import com.islandos.android.ui.viewmodel.MainViewModel
import kotlin.math.roundToInt

private val SwatchColors = listOf(
    0xFF000000L,
    0xFF0B0E17L,
    0xFF131722L,
    0xFF1A1426L,
    0xFF0B1C1AL,
    0xFF201414L
)

@Composable
fun CustomizationScreen(
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Customization",
            color = IslandColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        // Real-time Island Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF111521), Color(0xFF08090D))))
                .border(1.dp, IslandColors.HairlineBorder, RoundedCornerShape(20.dp))
                .padding(top = 16.dp, start = 12.dp, end = 12.dp),
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
        }

        // Theme Presets & Liquid Glass
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(IslandColors.SurfaceDark)
                .border(1.dp, IslandColors.HairlineBorder, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Themes & Liquid Glass", color = IslandColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IslandTheme.entries.forEach { preset ->
                    val selected = settings.themePreset == preset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) IslandColors.AccentBlue.copy(alpha = 0.2f) else IslandColors.SurfaceElevated)
                            .border(
                                1.dp,
                                if (selected) IslandColors.AccentBlue else IslandColors.HairlineBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.applyTheme(preset) }
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.displayName.split(" ").first(),
                            color = if (selected) IslandColors.AccentBlue else IslandColors.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Liquid Glass Effect", color = IslandColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Specular rim + RenderEffect blur on Android 12+", color = IslandColors.TextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.liquidGlassEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updateSettings { it.copy(liquidGlassEnabled = enabled) }
                    }
                )
            }

            Text("Background Color", color = IslandColors.TextSecondary, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SwatchColors.forEach { hex ->
                    val selected = settings.backgroundColorHex == hex
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) IslandColors.AccentBlue else IslandColors.HairlineBorder,
                                shape = CircleShape
                            )
                            .clickable {
                                viewModel.updateSettings { it.copy(backgroundColorHex = hex) }
                            }
                    )
                }
            }
        }

        // Size & Positioning Sliders
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(IslandColors.SurfaceDark)
                .border(1.dp, IslandColors.HairlineBorder, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Geometry & Placement", color = IslandColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)

            LabeledSlider("Pill Width", "${settings.widthDp.roundToInt()} dp", settings.widthDp, 120f..320f) { v ->
                viewModel.updateSettings { it.copy(widthDp = v) }
            }
            LabeledSlider("Pill Height", "${settings.heightDp.roundToInt()} dp", settings.heightDp, 32f..56f) { v ->
                viewModel.updateSettings { it.copy(heightDp = v) }
            }
            LabeledSlider("Horizontal Offset X", "${settings.offsetXdp.roundToInt()} dp", settings.offsetXdp, -60f..60f) { v ->
                viewModel.updateSettings { it.copy(offsetXdp = v) }
            }
            LabeledSlider("Vertical Offset Y", "${settings.offsetYdp.roundToInt()} dp", settings.offsetYdp, 0f..64f) { v ->
                viewModel.updateSettings { it.copy(offsetYdp = v) }
            }
            LabeledSlider("Corner Radius", "${settings.cornerRadiusDp.roundToInt()} dp", settings.cornerRadiusDp, 12f..34f) { v ->
                viewModel.updateSettings { it.copy(cornerRadiusDp = v) }
            }
        }

        // Appearance & Animation Speed
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(IslandColors.SurfaceDark)
                .border(1.dp, IslandColors.HairlineBorder, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Motion & Typography", color = IslandColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)

            LabeledSlider("Transparency", "${(settings.transparencyAlpha * 100).roundToInt()}%", settings.transparencyAlpha, 0.55f..1.0f) { v ->
                viewModel.updateSettings { it.copy(transparencyAlpha = v) }
            }
            LabeledSlider("Text Scale", "${"%.2f".format(settings.textSizeScale)}x", settings.textSizeScale, 0.85f..1.25f) { v ->
                viewModel.updateSettings { it.copy(textSizeScale = v) }
            }
            LabeledSlider("Animation Speed", "${"%.2f".format(settings.animationSpeedScale)}x", settings.animationSpeedScale, 0.6f..1.6f) { v ->
                viewModel.updateSettings { it.copy(animationSpeedScale = v) }
            }
            LabeledSlider("Notification Duration", "${settings.notificationDurationSec}s", settings.notificationDurationSec.toFloat(), 3f..15f) { v ->
                viewModel.updateSettings { it.copy(notificationDurationSec = v.roundToInt()) }
            }
        }

        OutlinedButton(
            onClick = { viewModel.resetCustomization() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Reset to Default Dimensions")
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = IslandColors.TextSecondary, fontSize = 13.sp)
            Text(valueLabel, color = IslandColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range
        )
    }
}
