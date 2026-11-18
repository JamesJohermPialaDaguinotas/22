package com.islandos.android.system

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.islandos.android.model.LiveActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConnectivityMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val _connectivityState = MutableStateFlow(readCurrentSnapshot())
    val connectivityState: StateFlow<LiveActivity.ConnectivityActivity> = _connectivityState.asStateFlow()

    private val _transientBannerEvent = MutableStateFlow(false)
    val transientBannerEvent: StateFlow<Boolean> = _transientBannerEvent.asStateFlow()

    private var registered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            refresh(triggerBanner = true)
        }

        override fun onLost(network: Network) {
            refresh(triggerBanner = true)
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            refresh(triggerBanner = false)
        }
    }

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED,
                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED,
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> refresh(triggerBanner = true)
            }
        }
    }

    fun start() {
        if (registered) return
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        runCatching { connectivityManager.registerNetworkCallback(request, networkCallback) }

        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        }
        ContextCompat.registerReceiver(
            context,
            systemReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        registered = true
        refresh(triggerBanner = false)
    }

    fun stop() {
        if (!registered) return
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        runCatching { context.unregisterReceiver(systemReceiver) }
        registered = false
    }

    fun clearBannerEvent() {
        _transientBannerEvent.value = false
    }

    fun refresh(triggerBanner: Boolean = false) {
        val next = readCurrentSnapshot()
        if (triggerBanner && next != _connectivityState.value) {
            _transientBannerEvent.value = true
        }
        _connectivityState.value = next
    }

    @SuppressLint("MissingPermission")
    private fun readCurrentSnapshot(): LiveActivity.ConnectivityActivity {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        val wifiConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val wifiLabel = if (wifiConnected) {
            val rawSsid = runCatching {
                @Suppress("DEPRECATION")
                wifiManager?.connectionInfo?.ssid?.replace("\"", "")
            }.getOrNull()
            if (!rawSsid.isNullOrBlank() && rawSsid != "<unknown ssid>") rawSsid else "Wi-Fi Connected"
        } else {
            "Wi-Fi Off"
        }

        val adapter = bluetoothManager?.adapter
        val btEnabled = adapter?.isEnabled == true
        val btConnectedCount = if (btEnabled && PermissionHelper.hasBluetoothConnectPermission(context)) {
            runCatching {
                val headsetState = adapter?.getProfileConnectionState(BluetoothProfile.HEADSET)
                val a2dpState = adapter?.getProfileConnectionState(BluetoothProfile.A2DP)
                var count = 0
                if (headsetState == BluetoothProfile.STATE_CONNECTED) count++
                if (a2dpState == BluetoothProfile.STATE_CONNECTED && headsetState != BluetoothProfile.STATE_CONNECTED) count++
                count
            }.getOrDefault(0)
        } else {
            0
        }

        val airplaneMode = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) == 1

        return LiveActivity.ConnectivityActivity(
            wifiConnected = wifiConnected,
            wifiLabel = wifiLabel,
            bluetoothEnabled = btEnabled,
            bluetoothConnectedCount = btConnectedCount,
            airplaneMode = airplaneMode
        )
    }
}
