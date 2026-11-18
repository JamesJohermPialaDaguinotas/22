package com.islandos.android.service

import android.app.PendingIntent
import android.content.Context
import com.islandos.android.data.IslandSettings
import com.islandos.android.data.SettingsRepository
import com.islandos.android.model.ActivityType
import com.islandos.android.model.DynamicIslandUiState
import com.islandos.android.model.IslandMode
import com.islandos.android.model.LiveActivity
import com.islandos.android.system.BatteryMonitor
import com.islandos.android.system.CallMonitor
import com.islandos.android.system.ConnectivityMonitor
import com.islandos.android.system.MediaMonitor
import com.islandos.android.system.TimerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DynamicIslandStateCoordinator(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val batteryMonitor = BatteryMonitor(context)
    val connectivityMonitor = ConnectivityMonitor(context)
    val mediaMonitor = MediaMonitor(context)
    val callMonitor = CallMonitor(context)
    val timerManager = TimerManager()

    private val _notifications = MutableStateFlow<List<LiveActivity.NotificationActivity>>(emptyList())
    private val _activeAlertNotificationId = MutableStateFlow<String?>(null)
    private val _manualModeOverride = MutableStateFlow<IslandMode?>(null)
    private val _preferredActivityType = MutableStateFlow<ActivityType?>(null)
    private val _isScreenLocked = MutableStateFlow(false)
    private val _isLandscape = MutableStateFlow(false)

    private var monitorsRunning = false
    private var notificationBurstJob: Job? = null
    private var autoCollapseJob: Job? = null
    private var batteryPulseJob: Job? = null
    private var connectivityPulseJob: Job? = null

    val settingsFlow: StateFlow<IslandSettings> = settingsRepository.settingsFlow
        .stateIn(scope, SharingStarted.Eagerly, IslandSettings())

    private val internalPulseCombine = combine(
        _notifications,
        _activeAlertNotificationId,
        _manualModeOverride,
        _preferredActivityType
    ) { notifications, alertId, modeOverride, preferredType ->
        Quad(notifications, alertId, modeOverride, preferredType)
    }

    private val deviceCombine = combine(
        batteryMonitor.batteryState,
        batteryMonitor.justPluggedEvent,
        connectivityMonitor.connectivityState,
        connectivityMonitor.transientBannerEvent
    ) { battery, pluggedPulse, connectivity, connPulse ->
        DeviceSnapshot(battery, pluggedPulse, connectivity, connPulse)
    }

    private val liveCombine = combine(
        mediaMonitor.mediaState,
        timerManager.timerState,
        callMonitor.callState,
        _isScreenLocked,
        _isLandscape
    ) { media, timer, call, locked, landscape ->
        LiveSnapshot(media, timer, call, locked, landscape)
    }

    val uiState: StateFlow<DynamicIslandUiState> = combine(
        settingsFlow,
        internalPulseCombine,
        deviceCombine,
        liveCombine
    ) { settings, notifState, device, live ->
        buildUiState(settings, notifState, device, live)
    }.stateIn(scope, SharingStarted.Eagerly, DynamicIslandUiState())

    fun startMonitors() {
        if (monitorsRunning) return
        monitorsRunning = true
        batteryMonitor.start()
        connectivityMonitor.start()
        mediaMonitor.start()
        callMonitor.start()
    }

    fun stopMonitors() {
        if (!monitorsRunning) return
        monitorsRunning = false
        batteryMonitor.stop()
        connectivityMonitor.stop()
        mediaMonitor.stop()
        callMonitor.stop()
    }

    fun onNotificationListenerConnected() {
        if (monitorsRunning) {
            mediaMonitor.start()
        }
    }

    fun refreshMediaSessions() {
        mediaMonitor.refreshSessions()
    }

    fun onNotificationPosted(item: LiveActivity.NotificationActivity, burstToIsland: Boolean) {
        val current = _notifications.value.filterNot { it.notificationKey == item.notificationKey }
        val updated = (listOf(item) + current).take(12)
        _notifications.value = updated

        if (burstToIsland && settingsFlow.value.notificationsEnabled) {
            _activeAlertNotificationId.value = item.notificationKey
            notificationBurstJob?.cancel()
            notificationBurstJob = scope.launch {
                val seconds = settingsFlow.value.notificationDurationSec.coerceIn(2, 20)
                delay(seconds * 1000L)
                _activeAlertNotificationId.value = null
            }
        }
    }

    fun onNotificationRemoved(key: String) {
        _notifications.value = _notifications.value.filterNot { it.notificationKey == key }
        if (_activeAlertNotificationId.value == key) {
            _activeAlertNotificationId.value = null
        }
    }

    fun dismissNotification(key: String) {
        IslandNotificationListenerService.dismissNotification(key)
        onNotificationRemoved(key)
    }

    fun openNotificationIntent(item: LiveActivity.NotificationActivity) {
        runCatching { item.contentIntent?.send() }
        collapse()
    }

    fun onCallNotificationPosted(callerTitle: String, subtitle: String, contentIntent: PendingIntent?) {
        callMonitor.onCallNotificationUpdated(callerTitle, subtitle, contentIntent)
    }

    fun onCallNotificationRemoved() {
        callMonitor.onCallNotificationRemoved()
    }

    fun updateEnvironmentState(isLocked: Boolean, isLandscape: Boolean) {
        _isScreenLocked.value = isLocked
        _isLandscape.value = isLandscape
    }

    fun toggleExpanded() {
        val state = uiState.value
        if (state.mode == IslandMode.EXPANDED) {
            collapse()
        } else if (state.primaryActivity != null || state.battery != null) {
            expand()
        }
    }

    fun expand() {
        _manualModeOverride.value = IslandMode.EXPANDED
        scheduleAutoCollapse()
    }

    fun collapse() {
        autoCollapseJob?.cancel()
        _manualModeOverride.value = null
    }

    fun cycleWidget() {
        val list = uiState.value.allActivities
        if (list.size <= 1) {
            toggleExpanded()
            return
        }
        val current = uiState.value.primaryActivity
        val idx = list.indexOfFirst { it.type == current?.type }
        val next = list[(idx + 1) % list.size]
        _preferredActivityType.value = next.type
        scheduleAutoCollapse()
    }

    private fun scheduleAutoCollapse() {
        autoCollapseJob?.cancel()
        val delaySec = settingsFlow.value.autoCollapseSec
        if (delaySec <= 0) return
        autoCollapseJob = scope.launch {
            delay(delaySec * 1000L)
            _manualModeOverride.value = null
        }
    }

    private fun buildUiState(
        settings: IslandSettings,
        notifState: Quad,
        device: DeviceSnapshot,
        live: LiveSnapshot
    ): DynamicIslandUiState {
        if (!settings.isEnabled) {
            return DynamicIslandUiState(isServiceEnabled = false, mode = IslandMode.HIDDEN)
        }
        if (settings.hideInLandscape && live.isLandscape) {
            return DynamicIslandUiState(isServiceEnabled = true, mode = IslandMode.HIDDEN, isLandscape = true)
        }
        if (!settings.showOnLockScreen && live.isScreenLocked) {
            return DynamicIslandUiState(isServiceEnabled = true, mode = IslandMode.HIDDEN, isScreenLocked = true)
        }

        if (device.pluggedPulse) {
            batteryPulseJob?.cancel()
            batteryPulseJob = scope.launch {
                delay(4000L)
                batteryMonitor.clearPluggedPulse()
            }
        }
        if (device.connectivityPulse) {
            connectivityPulseJob?.cancel()
            connectivityPulseJob = scope.launch {
                delay(3500L)
                connectivityMonitor.clearBannerEvent()
            }
        }

        val candidates = mutableListOf<LiveActivity>()

        if (settings.callsEnabled && live.call != null) {
            candidates.add(live.call)
        }
        if (settings.notificationsEnabled && notifState.notifications.isNotEmpty()) {
            val activeNotif = notifState.notifications.firstOrNull { it.notificationKey == notifState.alertId }
                ?: notifState.notifications.firstOrNull()
            if (activeNotif != null && (notifState.alertId != null || notifState.modeOverride == IslandMode.EXPANDED)) {
                val redacted = if (live.isScreenLocked && settings.privacyRedactNotifications) {
                    activeNotif.copy(text = "Notification content hidden on lock screen")
                } else {
                    activeNotif
                }
                candidates.add(redacted.copy(totalQueuedCount = notifState.notifications.size))
            }
        }
        if (settings.timerEnabled && live.timer != null) {
            candidates.add(live.timer)
        }
        if (settings.musicEnabled && live.media != null) {
            candidates.add(live.media)
        }
        if (settings.batteryEnabled && device.battery != null && (device.pluggedPulse || device.battery.isLowBattery || notifState.modeOverride == IslandMode.EXPANDED)) {
            candidates.add(device.battery)
        }
        if (settings.connectivityEnabled && (device.connectivityPulse || device.connectivity.airplaneMode)) {
            candidates.add(device.connectivity)
        }

        val preferred = notifState.preferredType?.let { type ->
            candidates.firstOrNull { it.type == type }
        }
        val primary = preferred ?: candidates.firstOrNull() ?: if (settings.batteryEnabled && !settings.autoHideWhenIdle) {
            device.battery
        } else {
            null
        }

        val mode = when {
            primary == null && settings.autoHideWhenIdle -> IslandMode.HIDDEN
            notifState.modeOverride == IslandMode.EXPANDED && primary != null -> IslandMode.EXPANDED
            primary != null -> IslandMode.COMPACT
            else -> IslandMode.IDLE
        }

        return DynamicIslandUiState(
            isServiceEnabled = true,
            mode = mode,
            primaryActivity = primary,
            allActivities = candidates,
            activeNotifications = notifState.notifications,
            battery = device.battery,
            connectivity = device.connectivity,
            isScreenLocked = live.isScreenLocked,
            isLandscape = live.isLandscape
        )
    }

    private data class Quad(
        val notifications: List<LiveActivity.NotificationActivity>,
        val alertId: String?,
        val modeOverride: IslandMode?,
        val preferredType: ActivityType?
    )

    private data class DeviceSnapshot(
        val battery: LiveActivity.BatteryActivity?,
        val pluggedPulse: Boolean,
        val connectivity: LiveActivity.ConnectivityActivity,
        val connectivityPulse: Boolean
    )

    private data class LiveSnapshot(
        val media: LiveActivity.MusicActivity?,
        val timer: LiveActivity.TimerActivity?,
        val call: LiveActivity.CallActivity?,
        val isScreenLocked: Boolean,
        val isLandscape: Boolean
    )
}
