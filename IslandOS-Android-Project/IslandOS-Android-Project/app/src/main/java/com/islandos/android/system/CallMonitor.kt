package com.islandos.android.system

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.islandos.android.model.LiveActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Monitors real device call states within Android's security model.
 *
 * Platform constraints documented honestly:
 * - On Android 10+, third-party apps that are NOT the default Dialer (InCallService) cannot silently
 *   answer or hang up arbitrary carrier calls without privileged roles.
 * - When READ_PHONE_STATE is granted, we observe real TelephonyCallback / PhoneStateListener states.
 * - Additionally, when NotificationListenerService observes a Notification.CATEGORY_CALL notification,
 *   we enrich caller display name and open the legitimate Dialer UI via contentIntent / TelecomManager.
 */
class CallMonitor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    private val _callState = MutableStateFlow<LiveActivity.CallActivity?>(null)
    val callState: StateFlow<LiveActivity.CallActivity?> = _callState.asStateFlow()

    private val _callElapsedSeconds = MutableStateFlow(0L)
    val callElapsedSeconds: StateFlow<Long> = _callElapsedSeconds.asStateFlow()

    private var registeredTelephony = false
    private var api31Callback: Any? = null
    @Suppress("DEPRECATION")
    private var legacyListener: PhoneStateListener? = null
    private var durationJob: Job? = null

    private var lastNotificationCaller: String? = null
    private var lastNotificationSubtitle: String? = null
    private var lastNotificationIntent: PendingIntent? = null

    fun start() {
        if (registeredTelephony) return
        if (!PermissionHelper.hasPhoneStatePermission(context) || telephonyManager == null) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                registerApi31Callback(telephonyManager)
            } else {
                registerLegacyListener(telephonyManager)
            }
            registeredTelephony = true
        } catch (_: SecurityException) {
            registeredTelephony = false
        }
    }

    fun stop() {
        durationJob?.cancel()
        if (registeredTelephony && telephonyManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (api31Callback as? TelephonyCallback)?.let {
                    telephonyManager.unregisterTelephonyCallback(it)
                }
            } else {
                @Suppress("DEPRECATION")
                legacyListener?.let {
                    telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
                }
            }
        }
        registeredTelephony = false
        _callState.value = null
    }

    fun onCallNotificationUpdated(
        callerTitle: String,
        subtitle: String,
        contentIntent: PendingIntent?
    ) {
        lastNotificationCaller = callerTitle
        lastNotificationSubtitle = subtitle
        lastNotificationIntent = contentIntent

        val current = _callState.value
        if (current != null) {
            _callState.value = current.copy(
                callerDisplay = callerTitle.ifBlank { current.callerDisplay },
                subtitle = subtitle.ifBlank { current.subtitle },
                contentIntent = contentIntent ?: current.contentIntent
            )
        } else {
            _callState.value = LiveActivity.CallActivity(
                callerDisplay = callerTitle.ifBlank { "Incoming Call" },
                subtitle = subtitle.ifBlank { "Phone Call" },
                status = LiveActivity.CallActivity.CallStatus.RINGING,
                startedAtMs = SystemClock.elapsedRealtime(),
                canControlViaTelecom = false,
                contentIntent = contentIntent
            )
        }
    }

    fun onCallNotificationRemoved() {
        lastNotificationCaller = null
        lastNotificationSubtitle = null
        lastNotificationIntent = null
        if (!registeredTelephony) {
            durationJob?.cancel()
            _callState.value = null
        }
    }

    @SuppressLint("MissingPermission")
    fun openInCallScreen() {
        val pending = _callState.value?.contentIntent ?: lastNotificationIntent
        if (pending != null) {
            val sent = runCatching { pending.send() }.isSuccess
            if (sent) return
        }
        if (PermissionHelper.hasPhoneStatePermission(context)) {
            runCatching {
                telecomManager?.showInCallScreen(false)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerApi31Callback(tm: TelephonyManager) {
        val cb = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                handleTelephonyState(state)
            }
        }
        api31Callback = cb
        tm.registerTelephonyCallback(context.mainExecutor, cb)
    }

    @Suppress("DEPRECATION")
    private fun registerLegacyListener(tm: TelephonyManager) {
        val listener = object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                handleTelephonyState(state)
            }
        }
        legacyListener = listener
        tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun handleTelephonyState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                durationJob?.cancel()
                _callElapsedSeconds.value = 0L
                _callState.value = LiveActivity.CallActivity(
                    callerDisplay = lastNotificationCaller ?: "Incoming Call",
                    subtitle = lastNotificationSubtitle ?: "Tap to open dialer",
                    status = LiveActivity.CallActivity.CallStatus.RINGING,
                    startedAtMs = SystemClock.elapsedRealtime(),
                    canControlViaTelecom = true,
                    contentIntent = lastNotificationIntent
                )
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> {
                val startMs = SystemClock.elapsedRealtime()
                _callState.value = LiveActivity.CallActivity(
                    callerDisplay = lastNotificationCaller ?: "Active Call",
                    subtitle = lastNotificationSubtitle ?: "In progress",
                    status = LiveActivity.CallActivity.CallStatus.ACTIVE,
                    startedAtMs = startMs,
                    canControlViaTelecom = true,
                    contentIntent = lastNotificationIntent
                )
                startCallTimer(startMs)
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                durationJob?.cancel()
                _callElapsedSeconds.value = 0L
                _callState.value = null
            }
        }
    }

    private fun startCallTimer(startMs: Long) {
        durationJob?.cancel()
        durationJob = scope.launch {
            while (isActive) {
                _callElapsedSeconds.value = ((SystemClock.elapsedRealtime() - startMs) / 1000L).coerceAtLeast(0L)
                delay(1000L)
            }
        }
    }
}
