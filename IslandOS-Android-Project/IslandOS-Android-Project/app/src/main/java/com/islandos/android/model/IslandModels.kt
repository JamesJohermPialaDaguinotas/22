package com.islandos.android.model

import android.app.PendingIntent
import android.graphics.Bitmap

enum class IslandTheme(val displayName: String, val defaultBgColor: Long, val defaultAlpha: Float) {
    PURE_OLED("Pure OLED", 0xFF000000, 1.0f),
    LIQUID_GLASS("Liquid Glass", 0xFF0B0E17, 0.72f),
    MIDNIGHT_CARBON("Midnight Carbon", 0xFF11131A, 0.94f),
    FROSTED_TITANIUM("Frosted Titanium", 0xFF1C2029, 0.82f)
}

enum class IslandMode {
    HIDDEN,
    IDLE,
    COMPACT,
    EXPANDED
}

enum class ActivityType {
    CALL,
    NOTIFICATION,
    TIMER,
    MUSIC,
    BATTERY,
    CONNECTIVITY
}

sealed interface LiveActivity {
    val type: ActivityType
    val id: String

    data class MusicActivity(
        override val id: String = "music_session",
        val packageName: String,
        val appName: String,
        val title: String,
        val artist: String,
        val albumArt: Bitmap?,
        val isPlaying: Boolean,
        val positionMs: Long,
        val durationMs: Long
    ) : LiveActivity {
        override val type: ActivityType = ActivityType.MUSIC
        val progressFraction: Float
            get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }

    data class NotificationActivity(
        override val id: String,
        val notificationKey: String,
        val packageName: String,
        val appName: String,
        val title: String,
        val text: String,
        val appIcon: Bitmap?,
        val timestampMs: Long,
        val isClearable: Boolean,
        val contentIntent: PendingIntent?,
        val totalQueuedCount: Int = 1
    ) : LiveActivity {
        override val type: ActivityType = ActivityType.NOTIFICATION
    }

    data class BatteryActivity(
        override val id: String = "battery_state",
        val levelPercent: Int,
        val isCharging: Boolean,
        val chargingSource: String,
        val temperatureCelsius: Float,
        val voltageMv: Int,
        val isLowBattery: Boolean
    ) : LiveActivity {
        override val type: ActivityType = ActivityType.BATTERY
    }

    data class TimerActivity(
        override val id: String = "timer_session",
        val label: String,
        val isStopwatch: Boolean,
        val totalDurationMs: Long,
        val remainingOrElapsedMs: Long,
        val isRunning: Boolean,
        val isFinished: Boolean = false
    ) : LiveActivity {
        override val type: ActivityType = ActivityType.TIMER
        val progressFraction: Float
            get() = if (!isStopwatch && totalDurationMs > 0L) {
                (remainingOrElapsedMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                ((remainingOrElapsedMs % 60_000L) / 60_000f)
            }
    }

    enum class CallStatus { RINGING, ACTIVE, ENDED }

    data class CallActivity(
        override val id: String = "call_session",
        val callerDisplay: String,
        val subtitle: String,
        val status: CallStatus,
        val startedAtMs: Long,
        val canControlViaTelecom: Boolean,
        val contentIntent: PendingIntent? = null
    ) : LiveActivity {
        override val type: ActivityType = ActivityType.CALL
    }

    data class ConnectivityActivity(
        override val id: String = "connectivity_state",
        val wifiConnected: Boolean,
        val wifiLabel: String,
        val bluetoothEnabled: Boolean,
        val bluetoothConnectedCount: Int,
        val airplaneMode: Boolean
    ) : LiveActivity {
        override val type: ActivityType = ActivityType.CONNECTIVITY
    }
}

data class DynamicIslandUiState(
    val isServiceEnabled: Boolean = false,
    val mode: IslandMode = IslandMode.IDLE,
    val primaryActivity: LiveActivity? = null,
    val allActivities: List<LiveActivity> = emptyList(),
    val activeNotifications: List<LiveActivity.NotificationActivity> = emptyList(),
    val battery: LiveActivity.BatteryActivity? = null,
    val connectivity: LiveActivity.ConnectivityActivity? = null,
    val isScreenLocked: Boolean = false,
    val isLandscape: Boolean = false
)
