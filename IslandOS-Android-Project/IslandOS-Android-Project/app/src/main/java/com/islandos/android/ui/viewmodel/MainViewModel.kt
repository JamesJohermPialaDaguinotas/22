package com.islandos.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.islandos.android.DynamicIslandApp
import com.islandos.android.data.IslandSettings
import com.islandos.android.model.DynamicIslandUiState
import com.islandos.android.model.IslandTheme
import com.islandos.android.service.DynamicIslandOverlayService
import com.islandos.android.system.PermissionHelper
import com.islandos.android.system.PermissionSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DynamicIslandApp
    private val settingsRepo = app.settingsRepository
    val coordinator = app.coordinator

    val settingsFlow: StateFlow<IslandSettings> = coordinator.settingsFlow
    val uiStateFlow: StateFlow<DynamicIslandUiState> = coordinator.uiState

    private val _permissions = MutableStateFlow(PermissionHelper.snapshot(application))
    val permissions: StateFlow<PermissionSnapshot> = _permissions.asStateFlow()

    fun refreshPermissions() {
        val snap = PermissionHelper.snapshot(app)
        _permissions.value = snap
        if (!snap.overlayGranted && settingsFlow.value.isEnabled) {
            setIslandEnabled(false)
        }
        if (snap.notificationListenerGranted) {
            coordinator.refreshMediaSessions()
        }
    }

    fun setIslandEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !PermissionHelper.hasOverlayPermission(app)) {
                return@launch
            }
            settingsRepo.update { it.copy(isEnabled = enabled) }
            if (enabled) {
                DynamicIslandOverlayService.start(app)
            } else {
                DynamicIslandOverlayService.stop(app)
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepo.update { it.copy(onboardingCompleted = true) }
        }
    }

    fun updateSettings(transform: (IslandSettings) -> IslandSettings) {
        viewModelScope.launch {
            settingsRepo.update(transform)
        }
    }

    fun applyTheme(theme: IslandTheme) {
        viewModelScope.launch {
            settingsRepo.applyTheme(theme)
        }
    }

    fun resetCustomization() {
        viewModelScope.launch {
            settingsRepo.resetDefaults()
        }
    }

    fun startFocusTimer(minutes: Int) {
        coordinator.timerManager.startCountdown(minutes * 60_000L, "$minutes min Timer")
    }

    fun startStopwatch() {
        coordinator.timerManager.startStopwatch("Stopwatch")
    }
}
