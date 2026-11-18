package com.islandos.android.ui.overlay

import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.islandos.android.data.IslandSettings
import com.islandos.android.model.DynamicIslandUiState
import com.islandos.android.model.IslandMode
import com.islandos.android.model.LiveActivity
import com.islandos.android.ui.theme.IslandColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandOverlayContent(
    uiState: DynamicIslandUiState,
    settings: IslandSettings,
    onTapIsland: () -> Unit,
    onLongPressIsland: () -> Unit,
    onCollapse: () -> Unit,
    onPlayPauseMusic: () -> Unit,
    onNextMusic: () -> Unit,
    onPreviousMusic: () -> Unit,
    onSeekMusic: (Long) -> Unit,
    onDismissNotification: (String) -> Unit,
    onOpenNotification: (LiveActivity.NotificationActivity) -> Unit,
    onToggleTimer: () -> Unit,
    onAddTimerMinute: () -> Unit,
    onStopTimer: () -> Unit,
    onOpenCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.mode == IslandMode.HIDDEN) return

    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val stiffness = (Spring.StiffnessMediumLow * settings.animationSpeedScale).coerceIn(180f, 900f)
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = stiffness),
        label = "press_scale"
    )

    val targetWidth = when (uiState.mode) {
        IslandMode.EXPANDED -> 352.dp
        IslandMode.COMPACT -> maxOf(settings.widthDp.dp, 210.dp)
        IslandMode.IDLE -> settings.widthDp.dp
        IslandMode.HIDDEN -> 0.dp
    }
    val targetHeight = when (uiState.mode) {
        IslandMode.EXPANDED -> when (uiState.primaryActivity) {
            is LiveActivity.MusicActivity -> 186.dp
            is LiveActivity.NotificationActivity -> 148.dp
            is LiveActivity.TimerActivity -> 136.dp
            is LiveActivity.CallActivity -> 132.dp
            else -> 124.dp
        }
        else -> settings.heightDp.dp
    }
    val targetRadius = when (uiState.mode) {
        IslandMode.EXPANDED -> (settings.cornerRadiusDp + 10f).coerceIn(20f, 38f).dp
        else -> settings.cornerRadiusDp.dp
    }

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = stiffness),
        label = "island_width"
    )
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = stiffness),
        label = "island_height"
    )
    val animatedRadius by animateDpAsState(
        targetValue = targetRadius,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = stiffness),
        label = "island_radius"
    )

    val baseBg = Color(settings.backgroundColorHex)
    val surfaceAlpha = if (settings.liquidGlassEnabled) {
        settings.transparencyAlpha.coerceAtMost(0.84f)
    } else {
        settings.transparencyAlpha
    }
    val borderBrush = if (settings.liquidGlassEnabled) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.42f),
                Color.White.copy(alpha = 0.12f),
                Color(0xFF3B82F6).copy(alpha = 0.28f)
            ),
            start = Offset.Zero,
            end = Offset(400f, 200f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.06f)
            )
        )
    }

    val shape = RoundedCornerShape(animatedRadius)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .width(animatedWidth)
                .height(animatedHeight)
                .scale(pressScale)
                .clip(shape)
                .background(baseBg.copy(alpha = surfaceAlpha))
                .border(
                    width = if (settings.liquidGlassEnabled) 1.2.dp else 0.8.dp,
                    brush = borderBrush,
                    shape = shape
                )
                .pointerInput(uiState.mode) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -18f && uiState.mode == IslandMode.EXPANDED) {
                            onCollapse()
                        }
                    }
                }
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        if (settings.hapticFeedbackEnabled) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onTapIsland()
                    },
                    onLongClick = {
                        if (settings.hapticFeedbackEnabled) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onLongPressIsland()
                    }
                )
        ) {
            if (settings.liquidGlassEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            renderEffect = RenderEffect
                                .createBlurEffect(20f, 20f, Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        }
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            AnimatedContent(
                targetState = Pair(uiState.mode, uiState.primaryActivity?.type),
                transitionSpec = {
                    (fadeIn(tween((220 / settings.animationSpeedScale).toInt().coerceIn(90, 400))) +
                        scaleIn(initialScale = 0.92f)) togetherWith
                        (fadeOut(tween((160 / settings.animationSpeedScale).toInt().coerceIn(70, 320))) +
                            scaleOut(targetScale = 0.94f))
                },
                label = "island_content"
            ) { (mode, _) ->
                when (mode) {
                    IslandMode.IDLE -> IdlePillContent(uiState.battery)
                    IslandMode.COMPACT -> CompactIslandContent(
                        activity = uiState.primaryActivity,
                        textScale = settings.textSizeScale,
                        widgetCount = uiState.allActivities.size
                    )
                    IslandMode.EXPANDED -> ExpandedIslandContent(
                        uiState = uiState,
                        textScale = settings.textSizeScale,
                        onPlayPauseMusic = onPlayPauseMusic,
                        onNextMusic = onNextMusic,
                        onPreviousMusic = onPreviousMusic,
                        onSeekMusic = onSeekMusic,
                        onDismissNotification = onDismissNotification,
                        onOpenNotification = onOpenNotification,
                        onToggleTimer = onToggleTimer,
                        onAddTimerMinute = onAddTimerMinute,
                        onStopTimer = onStopTimer,
                        onOpenCall = onOpenCall,
                        onCollapse = onCollapse
                    )
                    IslandMode.HIDDEN -> Unit
                }
            }
        }
    }
}

@Composable
private fun IdlePillContent(battery: LiveActivity.BatteryActivity?) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFF151923))
                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
        )
        if (battery != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (battery.isCharging) {
                    Icon(
                        imageVector = Icons.Rounded.BatteryChargingFull,
                        contentDescription = null,
                        tint = IslandColors.AccentEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "${battery.levelPercent}%",
                    color = if (battery.isCharging) IslandColors.AccentEmerald else IslandColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CompactIslandContent(
    activity: LiveActivity?,
    textScale: Float,
    widgetCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        when (activity) {
            is LiveActivity.MusicActivity -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AlbumArtThumb(bitmap = activity.albumArt, sizeDp = 22)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = activity.title,
                        color = IslandColors.TextPrimary,
                        fontSize = (12f * textScale).sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                AudioWaveformBars(isPlaying = activity.isPlaying)
            }

            is LiveActivity.NotificationActivity -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AppIconThumb(bitmap = activity.appIcon, sizeDp = 22)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${activity.appName}: ${activity.title}",
                        color = IslandColors.TextPrimary,
                        fontSize = (12f * textScale).sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (activity.totalQueuedCount > 1) {
                    Text(
                        text = "+${activity.totalQueuedCount - 1}",
                        color = IslandColors.AccentBlue,
                        fontSize = (11f * textScale).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            is LiveActivity.TimerActivity -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressRing(
                        progress = activity.progressFraction,
                        color = if (activity.isFinished) IslandColors.AccentRose else IslandColors.AccentAmber,
                        sizeDp = 20
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = activity.label,
                        color = IslandColors.TextSecondary,
                        fontSize = (11f * textScale).sp,
                        maxLines = 1
                    )
                }
                Text(
                    text = formatDuration(activity.remainingOrElapsedMs),
                    color = if (activity.isFinished) IslandColors.AccentRose else IslandColors.AccentAmber,
                    fontSize = (13f * textScale).sp,
                    fontWeight = FontWeight.Bold
                )
            }

            is LiveActivity.BatteryActivity -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.BatteryChargingFull,
                        contentDescription = null,
                        tint = if (activity.isCharging) IslandColors.AccentEmerald else IslandColors.AccentAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = activity.chargingSource,
                        color = IslandColors.TextPrimary,
                        fontSize = (12f * textScale).sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "${activity.levelPercent}%",
                    color = if (activity.isCharging) IslandColors.AccentEmerald else IslandColors.AccentAmber,
                    fontSize = (13f * textScale).sp,
                    fontWeight = FontWeight.Bold
                )
            }

            is LiveActivity.CallActivity -> {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = null,
                        tint = IslandColors.AccentEmerald,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = activity.callerDisplay,
                        color = IslandColors.TextPrimary,
                        fontSize = (12f * textScale).sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = if (activity.status == LiveActivity.CallActivity.CallStatus.RINGING) "Incoming" else "Active",
                    color = IslandColors.AccentEmerald,
                    fontSize = (11f * textScale).sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            is LiveActivity.ConnectivityActivity -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (activity.airplaneMode) {
                        Icon(Icons.Rounded.AirplanemodeActive, null, tint = IslandColors.AccentAmber, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(
                            Icons.Rounded.Wifi,
                            null,
                            tint = if (activity.wifiConnected) IslandColors.AccentBlue else IslandColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Icon(
                        Icons.Rounded.Bluetooth,
                        null,
                        tint = if (activity.bluetoothEnabled) IslandColors.AccentBlue else IslandColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = if (activity.airplaneMode) "Airplane" else activity.wifiLabel,
                    color = IslandColors.TextPrimary,
                    fontSize = (11f * textScale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            null -> IdlePillContent(null)
        }

        if (widgetCount > 1) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.45f))
            )
        }
    }
}

@Composable
private fun ExpandedIslandContent(
    uiState: DynamicIslandUiState,
    textScale: Float,
    onPlayPauseMusic: () -> Unit,
    onNextMusic: () -> Unit,
    onPreviousMusic: () -> Unit,
    onSeekMusic: (Long) -> Unit,
    onDismissNotification: (String) -> Unit,
    onOpenNotification: (LiveActivity.NotificationActivity) -> Unit,
    onToggleTimer: () -> Unit,
    onAddTimerMinute: () -> Unit,
    onStopTimer: () -> Unit,
    onOpenCall: () -> Unit,
    onCollapse: () -> Unit
) {
    when (val activity = uiState.primaryActivity) {
        is LiveActivity.MusicActivity -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    AlbumArtThumb(bitmap = activity.albumArt, sizeDp = 50)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activity.title,
                            color = IslandColors.TextPrimary,
                            fontSize = (15f * textScale).sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${activity.artist} · ${activity.appName}",
                            color = IslandColors.TextSecondary,
                            fontSize = (12f * textScale).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    AudioWaveformBars(isPlaying = activity.isPlaying)
                }

                Column {
                    Slider(
                        value = activity.progressFraction,
                        onValueChange = { fraction ->
                            if (activity.durationMs > 0L) {
                                onSeekMusic((fraction * activity.durationMs).toLong())
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = IslandColors.AccentBlue,
                            inactiveTrackColor = Color.White.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.height(22.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatDuration(activity.positionMs), color = IslandColors.TextSecondary, fontSize = 10.sp)
                        Text(formatDuration(activity.durationMs), color = IslandColors.TextSecondary, fontSize = 10.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPreviousMusic, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.SkipPrevious, "Previous", tint = Color.White)
                    }
                    IconButton(
                        onClick = onPlayPauseMusic,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Icon(
                            imageVector = if (activity.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onNextMusic, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.SkipNext, "Next", tint = Color.White)
                    }
                }
            }
        }

        is LiveActivity.NotificationActivity -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    AppIconThumb(bitmap = activity.appIcon, sizeDp = 40)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activity.appName,
                            color = IslandColors.AccentBlue,
                            fontSize = (11f * textScale).sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = activity.title,
                            color = IslandColors.TextPrimary,
                            fontSize = (14f * textScale).sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (activity.isClearable) {
                        IconButton(
                            onClick = { onDismissNotification(activity.notificationKey) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Close, "Dismiss", tint = IslandColors.TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Text(
                    text = activity.text.ifBlank { "Tap Open to view notification" },
                    color = IslandColors.TextSecondary,
                    fontSize = (12.5f * textScale).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (activity.totalQueuedCount > 1) "${activity.totalQueuedCount} active notifications" else "Swipe up to collapse",
                        color = IslandColors.TextSecondary,
                        fontSize = 11.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.TextButton(onClick = onCollapse) {
                            Text("Close", color = IslandColors.TextSecondary, fontSize = 12.sp)
                        }
                        androidx.compose.material3.TextButton(onClick = { onOpenNotification(activity) }) {
                            Text("Open App", color = IslandColors.AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        is LiveActivity.TimerActivity -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressRing(
                        progress = activity.progressFraction,
                        color = if (activity.isFinished) IslandColors.AccentRose else IslandColors.AccentAmber,
                        sizeDp = 38
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activity.label,
                            color = IslandColors.TextSecondary,
                            fontSize = (12f * textScale).sp
                        )
                        Text(
                            text = formatDuration(activity.remainingOrElapsedMs),
                            color = IslandColors.TextPrimary,
                            fontSize = (22f * textScale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!activity.isStopwatch) {
                            androidx.compose.material3.TextButton(onClick = onAddTimerMinute) {
                                Text("+1m", color = IslandColors.AccentAmber, fontSize = 12.sp)
                            }
                        }
                        IconButton(
                            onClick = onToggleTimer,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(IslandColors.AccentAmber.copy(alpha = 0.22f))
                        ) {
                            Icon(
                                imageVector = if (activity.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Toggle Timer",
                                tint = IslandColors.AccentAmber
                            )
                        }
                        IconButton(
                            onClick = onStopTimer,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Icon(Icons.Rounded.Close, "Stop", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        is LiveActivity.CallActivity -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(IslandColors.AccentEmerald.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Call, null, tint = IslandColors.AccentEmerald)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = activity.callerDisplay,
                            color = IslandColors.TextPrimary,
                            fontSize = (15f * textScale).sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = activity.subtitle,
                            color = IslandColors.TextSecondary,
                            fontSize = (12f * textScale).sp,
                            maxLines = 1
                        )
                    }
                }
                androidx.compose.material3.Button(
                    onClick = onOpenCall,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = IslandColors.AccentEmerald,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Open Dialer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        is LiveActivity.BatteryActivity -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.BatteryChargingFull,
                            null,
                            tint = IslandColors.AccentEmerald,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(activity.chargingSource, color = IslandColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${activity.temperatureCelsius}°C · ${activity.voltageMv} mV",
                                color = IslandColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Text(
                        text = "${activity.levelPercent}%",
                        color = IslandColors.AccentEmerald,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { (activity.levelPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (activity.isCharging) IslandColors.AccentEmerald else IslandColors.AccentBlue,
                    trackColor = Color.White.copy(alpha = 0.14f)
                )
            }
        }

        is LiveActivity.ConnectivityActivity -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (activity.airplaneMode) "Airplane Mode On" else activity.wifiLabel,
                        color = IslandColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (activity.bluetoothEnabled) {
                            "Bluetooth On · ${activity.bluetoothConnectedCount} connected"
                        } else {
                            "Bluetooth Off"
                        },
                        color = IslandColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
                androidx.compose.material3.TextButton(onClick = onCollapse) {
                    Text("Done", color = IslandColors.AccentBlue)
                }
            }
        }

        null -> Unit
    }
}

@Composable
private fun AlbumArtThumb(bitmap: Bitmap?, sizeDp: Int) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(6.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(IslandColors.AccentBlue.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.MusicNote, null, tint = IslandColors.AccentBlue, modifier = Modifier.size((sizeDp * 0.6f).dp))
        }
    }
}

@Composable
private fun AppIconThumb(bitmap: Bitmap?, sizeDp: Int) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "App Icon",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .background(IslandColors.AccentBlue.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Notifications, null, tint = IslandColors.AccentBlue, modifier = Modifier.size((sizeDp * 0.6f).dp))
        }
    }
}

@Composable
private fun AudioWaveformBars(isPlaying: Boolean) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val phase1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val phase2 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val phase3 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar3"
    )

    val heights = if (isPlaying) listOf(phase1, phase2, phase3) else listOf(0.35f, 0.35f, 0.35f)
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(16.dp)
    ) {
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((14f * h).dp)
                    .clip(CircleShape)
                    .background(IslandColors.AccentBlue)
            )
        }
    }
}

@Composable
private fun CircularProgressRing(progress: Float, color: Color, sizeDp: Int) {
    Canvas(modifier = Modifier.size(sizeDp.dp)) {
        drawArc(
            color = Color.White.copy(alpha = 0.16f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
