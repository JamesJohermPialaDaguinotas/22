package com.islandos.android.system

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import com.islandos.android.model.LiveActivity
import com.islandos.android.service.IslandNotificationListenerService
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

class MediaMonitor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val listenerComponent =
        ComponentName(context, IslandNotificationListenerService::class.java)

    private val _mediaState = MutableStateFlow<LiveActivity.MusicActivity?>(null)
    val mediaState: StateFlow<LiveActivity.MusicActivity?> = _mediaState.asStateFlow()

    private var activeController: MediaController? = null
    private var progressTickerJob: Job? = null
    private var registered = false

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        bindBestController(controllers.orEmpty())
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publishFromController(activeController)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publishFromController(activeController)
        }

        override fun onSessionDestroyed() {
            refreshSessions()
        }
    }

    fun start() {
        if (registered) return
        if (!PermissionHelper.hasNotificationListenerAccess(context)) return
        runCatching {
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent)
            registered = true
            refreshSessions()
        }
    }

    fun stop() {
        progressTickerJob?.cancel()
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
        if (registered) {
            runCatching { mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsListener) }
            registered = false
        }
        _mediaState.value = null
    }

    fun refreshSessions() {
        if (!PermissionHelper.hasNotificationListenerAccess(context)) {
            _mediaState.value = null
            return
        }
        if (!registered) {
            start()
            return
        }
        runCatching {
            val controllers = mediaSessionManager.getActiveSessions(listenerComponent)
            bindBestController(controllers)
        }
    }

    fun togglePlayPause() {
        val controller = activeController ?: return
        val state = controller.playbackState?.state
        if (state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun skipToNext() {
        activeController?.transportControls?.skipToNext()
    }

    fun skipToPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        activeController?.transportControls?.seekTo(positionMs.coerceAtLeast(0L))
    }

    private fun bindBestController(controllers: List<MediaController>) {
        val preferred = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers.firstOrNull {
            val s = it.playbackState?.state
            s == PlaybackState.STATE_PAUSED || s == PlaybackState.STATE_BUFFERING
        } ?: controllers.firstOrNull()

        if (activeController?.sessionToken != preferred?.sessionToken) {
            activeController?.unregisterCallback(controllerCallback)
            activeController = preferred
            preferred?.registerCallback(controllerCallback)
        }
        publishFromController(preferred)
    }

    private fun publishFromController(controller: MediaController?) {
        progressTickerJob?.cancel()
        if (controller == null) {
            _mediaState.value = null
            return
        }

        val metadata = controller.metadata
        val playbackState = controller.playbackState
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        if (title.isBlank()) {
            _mediaState.value = null
            return
        }

        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: resolveAppName(controller.packageName)

        val albumArt: Bitmap? = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L
        val stateCode = playbackState?.state ?: PlaybackState.STATE_NONE
        val isPlaying = stateCode == PlaybackState.STATE_PLAYING || stateCode == PlaybackState.STATE_BUFFERING

        val computedPosition = computeInterpolatedPosition(playbackState, duration)

        _mediaState.value = LiveActivity.MusicActivity(
            packageName = controller.packageName,
            appName = resolveAppName(controller.packageName),
            title = title,
            artist = artist,
            albumArt = albumArt,
            isPlaying = isPlaying,
            positionMs = computedPosition,
            durationMs = duration
        )

        if (isPlaying && duration > 0L) {
            progressTickerJob = scope.launch {
                while (isActive) {
                    delay(500L)
                    val latestState = activeController?.playbackState ?: break
                    val currentPos = computeInterpolatedPosition(latestState, duration)
                    _mediaState.value = _mediaState.value?.copy(positionMs = currentPos)
                }
            }
        }
    }

    private fun computeInterpolatedPosition(state: PlaybackState?, duration: Long): Long {
        if (state == null) return 0L
        var position = state.position
        if (state.state == PlaybackState.STATE_PLAYING && state.lastPositionUpdateTime > 0L) {
            val timeDelta = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
            position += (timeDelta * state.playbackSpeed).toLong()
        }
        return if (duration > 0L) position.coerceIn(0L, duration) else position.coerceAtLeast(0L)
    }

    private fun resolveAppName(packageName: String): String {
        return runCatching {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
    }
}
