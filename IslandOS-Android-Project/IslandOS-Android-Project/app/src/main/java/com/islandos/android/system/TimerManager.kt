package com.islandos.android.system

import android.os.SystemClock
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

class TimerManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    private val _timerState = MutableStateFlow<LiveActivity.TimerActivity?>(null)
    val timerState: StateFlow<LiveActivity.TimerActivity?> = _timerState.asStateFlow()

    private var anchorElapsedRealtimeMs: Long = 0L
    private var baseStoredMs: Long = 0L

    fun startCountdown(durationMs: Long, label: String = "Focus Timer") {
        tickJob?.cancel()
        val clamped = durationMs.coerceAtLeast(1_000L)
        baseStoredMs = clamped
        anchorElapsedRealtimeMs = SystemClock.elapsedRealtime()
        _timerState.value = LiveActivity.TimerActivity(
            label = label,
            isStopwatch = false,
            totalDurationMs = clamped,
            remainingOrElapsedMs = clamped,
            isRunning = true,
            isFinished = false
        )
        startTicker()
    }

    fun startStopwatch(label: String = "Stopwatch") {
        tickJob?.cancel()
        baseStoredMs = 0L
        anchorElapsedRealtimeMs = SystemClock.elapsedRealtime()
        _timerState.value = LiveActivity.TimerActivity(
            label = label,
            isStopwatch = true,
            totalDurationMs = 60_000L,
            remainingOrElapsedMs = 0L,
            isRunning = true,
            isFinished = false
        )
        startTicker()
    }

    fun togglePauseResume() {
        val current = _timerState.value ?: return
        if (current.isFinished) {
            stopAndClear()
            return
        }
        if (current.isRunning) {
            val delta = SystemClock.elapsedRealtime() - anchorElapsedRealtimeMs
            baseStoredMs = if (current.isStopwatch) {
                baseStoredMs + delta
            } else {
                (baseStoredMs - delta).coerceAtLeast(0L)
            }
            tickJob?.cancel()
            _timerState.value = current.copy(
                remainingOrElapsedMs = baseStoredMs,
                isRunning = false
            )
        } else {
            anchorElapsedRealtimeMs = SystemClock.elapsedRealtime()
            _timerState.value = current.copy(isRunning = true)
            startTicker()
        }
    }

    fun addOneMinute() {
        val current = _timerState.value ?: return
        if (current.isStopwatch) return
        val newRemaining = current.remainingOrElapsedMs + 60_000L
        baseStoredMs += 60_000L
        _timerState.value = current.copy(
            totalDurationMs = current.totalDurationMs + 60_000L,
            remainingOrElapsedMs = newRemaining,
            isFinished = false
        )
        if (!current.isRunning) {
            anchorElapsedRealtimeMs = SystemClock.elapsedRealtime()
        }
    }

    fun reset() {
        val current = _timerState.value ?: return
        tickJob?.cancel()
        anchorElapsedRealtimeMs = SystemClock.elapsedRealtime()
        baseStoredMs = if (current.isStopwatch) 0L else current.totalDurationMs
        _timerState.value = current.copy(
            remainingOrElapsedMs = baseStoredMs,
            isRunning = false,
            isFinished = false
        )
    }

    fun stopAndClear() {
        tickJob?.cancel()
        _timerState.value = null
    }

    private fun startTicker() {
        tickJob = scope.launch {
            while (isActive) {
                val current = _timerState.value ?: break
                if (!current.isRunning) break
                val elapsedSinceAnchor = SystemClock.elapsedRealtime() - anchorElapsedRealtimeMs
                if (current.isStopwatch) {
                    _timerState.value = current.copy(
                        remainingOrElapsedMs = baseStoredMs + elapsedSinceAnchor
                    )
                } else {
                    val remaining = (baseStoredMs - elapsedSinceAnchor).coerceAtLeast(0L)
                    if (remaining == 0L) {
                        _timerState.value = current.copy(
                            remainingOrElapsedMs = 0L,
                            isRunning = false,
                            isFinished = true
                        )
                        break
                    } else {
                        _timerState.value = current.copy(remainingOrElapsedMs = remaining)
                    }
                }
                delay(100L)
            }
        }
    }
}
