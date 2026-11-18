package com.islandos.android.service

import android.app.KeyguardManager
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.islandos.android.DynamicIslandApp
import com.islandos.android.MainActivity
import com.islandos.android.R
import com.islandos.android.model.IslandMode
import com.islandos.android.system.PermissionHelper
import com.islandos.android.ui.overlay.DynamicIslandOverlayContent
import com.islandos.android.ui.theme.IslandOSTheme
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class DynamicIslandOverlayService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = store

    private lateinit var windowManager: WindowManager
    private lateinit var keyguardManager: KeyguardManager
    private var overlayView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val app: DynamicIslandApp
        get() = applicationContext as DynamicIslandApp

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF,
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> syncEnvironment()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        startAsForeground()

        if (!PermissionHelper.hasOverlayPermission(this)) {
            stopSelf()
            return
        }

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenReceiver, screenFilter, ContextCompat.RECEIVER_EXPORTED)

        app.coordinator.startMonitors()
        syncEnvironment()
        attachOverlayWindow()

        lifecycleScope.launch {
            combine(app.coordinator.settingsFlow, app.coordinator.uiState) { settings, uiState ->
                settings to uiState
            }.collect { (settings, uiState) ->
                if (!PermissionHelper.hasOverlayPermission(this@DynamicIslandOverlayService)) {
                    stopSelf()
                    return@collect
                }
                if (!settings.isEnabled) {
                    stopSelf()
                    return@collect
                }
                updateWindowGeometry(
                    mode = uiState.mode,
                    widthDp = settings.widthDp,
                    heightDp = settings.heightDp,
                    offsetXdp = settings.offsetXdp,
                    offsetYdp = settings.offsetYdp
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!PermissionHelper.hasOverlayPermission(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        syncEnvironment(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        detachOverlayWindow()
        app.coordinator.stopMonitors()
        store.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private fun startAsForeground() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            100,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, DynamicIslandApp.CHANNEL_OVERLAY_SERVICE)
            .setSmallIcon(R.drawable.ic_island_notification)
            .setContentTitle(getString(R.string.fgs_notification_title))
            .setContentText(getString(R.string.fgs_notification_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun attachOverlayWindow() {
        if (overlayView != null) return
        val density = resources.displayMetrics.density

        val params = WindowManager.LayoutParams(
            (180 * density).roundToInt(),
            (48 * density).roundToInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = (10 * density).roundToInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        layoutParams = params

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DynamicIslandOverlayService)
            setViewTreeViewModelStoreOwner(this@DynamicIslandOverlayService)
            setViewTreeSavedStateRegistryOwner(this@DynamicIslandOverlayService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val settings by app.coordinator.settingsFlow.collectAsState()
                val uiState by app.coordinator.uiState.collectAsState()
                IslandOSTheme {
                    DynamicIslandOverlayContent(
                        uiState = uiState,
                        settings = settings,
                        onTapIsland = { app.coordinator.toggleExpanded() },
                        onLongPressIsland = { app.coordinator.cycleWidget() },
                        onCollapse = { app.coordinator.collapse() },
                        onPlayPauseMusic = { app.coordinator.mediaMonitor.togglePlayPause() },
                        onNextMusic = { app.coordinator.mediaMonitor.skipToNext() },
                        onPreviousMusic = { app.coordinator.mediaMonitor.skipToPrevious() },
                        onSeekMusic = { ms -> app.coordinator.mediaMonitor.seekTo(ms) },
                        onDismissNotification = { key -> app.coordinator.dismissNotification(key) },
                        onOpenNotification = { item -> app.coordinator.openNotificationIntent(item) },
                        onToggleTimer = { app.coordinator.timerManager.togglePauseResume() },
                        onAddTimerMinute = { app.coordinator.timerManager.addOneMinute() },
                        onStopTimer = { app.coordinator.timerManager.stopAndClear() },
                        onOpenCall = { app.coordinator.callMonitor.openInCallScreen() }
                    )
                }
            }
        }

        overlayView = composeView
        runCatching { windowManager.addView(composeView, params) }
    }

    private fun updateWindowGeometry(
        mode: IslandMode,
        widthDp: Float,
        heightDp: Float,
        offsetXdp: Float,
        offsetYdp: Float
    ) {
        val view = overlayView ?: return
        val params = layoutParams ?: return
        val density = resources.displayMetrics.density
        val screenWidthPx = resources.displayMetrics.widthPixels

        val targetWidthPx: Int
        val targetHeightPx: Int
        when (mode) {
            IslandMode.HIDDEN -> {
                targetWidthPx = 1
                targetHeightPx = 1
            }
            IslandMode.EXPANDED -> {
                targetWidthPx = minOf((364 * density).roundToInt(), (screenWidthPx * 0.94f).roundToInt())
                targetHeightPx = (210 * density).roundToInt()
            }
            IslandMode.COMPACT -> {
                targetWidthPx = maxOf((widthDp * density).roundToInt(), (216 * density).roundToInt())
                targetHeightPx = ((heightDp + 8f) * density).roundToInt()
            }
            IslandMode.IDLE -> {
                targetWidthPx = (widthDp * density).roundToInt()
                targetHeightPx = ((heightDp + 6f) * density).roundToInt()
            }
        }

        params.width = targetWidthPx
        params.height = targetHeightPx
        params.x = (offsetXdp * density).roundToInt()
        params.y = (offsetYdp * density).roundToInt()

        runCatching {
            if (view.isAttachedToWindow) {
                windowManager.updateViewLayout(view, params)
            }
        }
    }

    private fun detachOverlayWindow() {
        overlayView?.let { view ->
            runCatching {
                if (view.isAttachedToWindow) {
                    windowManager.removeViewImmediate(view)
                }
            }
        }
        overlayView = null
    }

    private fun syncEnvironment(
        isLandscape: Boolean = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    ) {
        app.coordinator.updateEnvironmentState(
            isLocked = keyguardManager.isKeyguardLocked,
            isLandscape = isLandscape
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 4091

        fun start(context: Context) {
            if (!PermissionHelper.hasOverlayPermission(context)) return
            val intent = Intent(context, DynamicIslandOverlayService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DynamicIslandOverlayService::class.java))
        }
    }
}
