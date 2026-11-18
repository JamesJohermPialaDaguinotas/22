package com.islandos.android.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.islandos.android.model.LiveActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BatteryMonitor(private val context: Context) {

    private val _batteryState = MutableStateFlow<LiveActivity.BatteryActivity?>(null)
    val batteryState: StateFlow<LiveActivity.BatteryActivity?> = _batteryState.asStateFlow()

    private val _justPluggedEvent = MutableStateFlow(false)
    val justPluggedEvent: StateFlow<Boolean> = _justPluggedEvent.asStateFlow()

    private var wasCharging: Boolean? = null
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> parseBatteryIntent(intent)
                Intent.ACTION_POWER_CONNECTED -> {
                    _justPluggedEvent.value = true
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    _justPluggedEvent.value = false
                }
            }
        }
    }

    fun start() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        val stickyIntent = ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        stickyIntent?.let(::parseBatteryIntent)
        registered = true
    }

    fun stop() {
        if (!registered) return
        runCatching { context.unregisterReceiver(receiver) }
        registered = false
    }

    fun clearPluggedPulse() {
        _justPluggedEvent.value = false
    }

    private fun parseBatteryIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val pct = if (level >= 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else 0

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val source = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Fast AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> if (isCharging) "Charging" else "On Battery"
        }

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)

        if (wasCharging == false && isCharging) {
            _justPluggedEvent.value = true
        }
        wasCharging = isCharging

        _batteryState.value = LiveActivity.BatteryActivity(
            levelPercent = pct,
            isCharging = isCharging,
            chargingSource = source,
            temperatureCelsius = tempTenths / 10f,
            voltageMv = voltageMv,
            isLowBattery = !isCharging && pct <= 15
        )
    }
}
