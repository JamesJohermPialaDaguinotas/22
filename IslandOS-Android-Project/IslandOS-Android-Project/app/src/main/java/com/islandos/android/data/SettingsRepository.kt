package com.islandos.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.islandos.android.model.IslandTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.islandDataStore: DataStore<Preferences> by preferencesDataStore(name = "island_settings")

data class IslandSettings(
    val isEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false,
    // Geometry & Positioning
    val widthDp: Float = 148f,
    val heightDp: Float = 38f,
    val offsetXdp: Float = 0f,
    val offsetYdp: Float = 10f,
    val cornerRadiusDp: Float = 24f,
    // Appearance & Liquid Glass
    val backgroundColorHex: Long = 0xFF000000,
    val transparencyAlpha: Float = 0.96f,
    val textSizeScale: Float = 1.0f,
    val animationSpeedScale: Float = 1.0f,
    val notificationDurationSec: Int = 6,
    val themePreset: IslandTheme = IslandTheme.PURE_OLED,
    val liquidGlassEnabled: Boolean = false,
    // Feature Toggles
    val musicEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val batteryEnabled: Boolean = true,
    val timerEnabled: Boolean = true,
    val connectivityEnabled: Boolean = true,
    val callsEnabled: Boolean = true,
    // Behavior, Startup & Privacy
    val startOnBoot: Boolean = true,
    val autoCollapseSec: Int = 8,
    val autoHideWhenIdle: Boolean = false,
    val hideInLandscape: Boolean = true,
    val showOnLockScreen: Boolean = true,
    val privacyRedactNotifications: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val IS_ENABLED = booleanPreferencesKey("is_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val WIDTH_DP = floatPreferencesKey("width_dp")
        val HEIGHT_DP = floatPreferencesKey("height_dp")
        val OFFSET_X_DP = floatPreferencesKey("offset_x_dp")
        val OFFSET_Y_DP = floatPreferencesKey("offset_y_dp")
        val CORNER_RADIUS_DP = floatPreferencesKey("corner_radius_dp")
        val BG_COLOR_HEX = longPreferencesKey("bg_color_hex")
        val TRANSPARENCY_ALPHA = floatPreferencesKey("transparency_alpha")
        val TEXT_SIZE_SCALE = floatPreferencesKey("text_size_scale")
        val ANIMATION_SPEED_SCALE = floatPreferencesKey("animation_speed_scale")
        val NOTIFICATION_DURATION_SEC = intPreferencesKey("notification_duration_sec")
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val LIQUID_GLASS_ENABLED = booleanPreferencesKey("liquid_glass_enabled")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val BATTERY_ENABLED = booleanPreferencesKey("battery_enabled")
        val TIMER_ENABLED = booleanPreferencesKey("timer_enabled")
        val CONNECTIVITY_ENABLED = booleanPreferencesKey("connectivity_enabled")
        val CALLS_ENABLED = booleanPreferencesKey("calls_enabled")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val AUTO_COLLAPSE_SEC = intPreferencesKey("auto_collapse_sec")
        val AUTO_HIDE_WHEN_IDLE = booleanPreferencesKey("auto_hide_when_idle")
        val HIDE_IN_LANDSCAPE = booleanPreferencesKey("hide_in_landscape")
        val SHOW_ON_LOCK_SCREEN = booleanPreferencesKey("show_on_lock_screen")
        val PRIVACY_REDACT = booleanPreferencesKey("privacy_redact")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
    }

    val settingsFlow: Flow<IslandSettings> = context.islandDataStore.data.map { prefs ->
        val themeName = prefs[Keys.THEME_PRESET] ?: IslandTheme.PURE_OLED.name
        val theme = runCatching { IslandTheme.valueOf(themeName) }.getOrDefault(IslandTheme.PURE_OLED)
        IslandSettings(
            isEnabled = prefs[Keys.IS_ENABLED] ?: false,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            widthDp = prefs[Keys.WIDTH_DP] ?: 148f,
            heightDp = prefs[Keys.HEIGHT_DP] ?: 38f,
            offsetXdp = prefs[Keys.OFFSET_X_DP] ?: 0f,
            offsetYdp = prefs[Keys.OFFSET_Y_DP] ?: 10f,
            cornerRadiusDp = prefs[Keys.CORNER_RADIUS_DP] ?: 24f,
            backgroundColorHex = prefs[Keys.BG_COLOR_HEX] ?: 0xFF000000,
            transparencyAlpha = prefs[Keys.TRANSPARENCY_ALPHA] ?: 0.96f,
            textSizeScale = prefs[Keys.TEXT_SIZE_SCALE] ?: 1.0f,
            animationSpeedScale = prefs[Keys.ANIMATION_SPEED_SCALE] ?: 1.0f,
            notificationDurationSec = prefs[Keys.NOTIFICATION_DURATION_SEC] ?: 6,
            themePreset = theme,
            liquidGlassEnabled = prefs[Keys.LIQUID_GLASS_ENABLED] ?: false,
            musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            batteryEnabled = prefs[Keys.BATTERY_ENABLED] ?: true,
            timerEnabled = prefs[Keys.TIMER_ENABLED] ?: true,
            connectivityEnabled = prefs[Keys.CONNECTIVITY_ENABLED] ?: true,
            callsEnabled = prefs[Keys.CALLS_ENABLED] ?: true,
            startOnBoot = prefs[Keys.START_ON_BOOT] ?: true,
            autoCollapseSec = prefs[Keys.AUTO_COLLAPSE_SEC] ?: 8,
            autoHideWhenIdle = prefs[Keys.AUTO_HIDE_WHEN_IDLE] ?: false,
            hideInLandscape = prefs[Keys.HIDE_IN_LANDSCAPE] ?: true,
            showOnLockScreen = prefs[Keys.SHOW_ON_LOCK_SCREEN] ?: true,
            privacyRedactNotifications = prefs[Keys.PRIVACY_REDACT] ?: true,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK] ?: true
        )
    }

    suspend fun update(transform: (IslandSettings) -> IslandSettings) {
        context.islandDataStore.edit { prefs ->
            val current = IslandSettings(
                isEnabled = prefs[Keys.IS_ENABLED] ?: false,
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                widthDp = prefs[Keys.WIDTH_DP] ?: 148f,
                heightDp = prefs[Keys.HEIGHT_DP] ?: 38f,
                offsetXdp = prefs[Keys.OFFSET_X_DP] ?: 0f,
                offsetYdp = prefs[Keys.OFFSET_Y_DP] ?: 10f,
                cornerRadiusDp = prefs[Keys.CORNER_RADIUS_DP] ?: 24f,
                backgroundColorHex = prefs[Keys.BG_COLOR_HEX] ?: 0xFF000000,
                transparencyAlpha = prefs[Keys.TRANSPARENCY_ALPHA] ?: 0.96f,
                textSizeScale = prefs[Keys.TEXT_SIZE_SCALE] ?: 1.0f,
                animationSpeedScale = prefs[Keys.ANIMATION_SPEED_SCALE] ?: 1.0f,
                notificationDurationSec = prefs[Keys.NOTIFICATION_DURATION_SEC] ?: 6,
                themePreset = runCatching {
                    IslandTheme.valueOf(prefs[Keys.THEME_PRESET] ?: IslandTheme.PURE_OLED.name)
                }.getOrDefault(IslandTheme.PURE_OLED),
                liquidGlassEnabled = prefs[Keys.LIQUID_GLASS_ENABLED] ?: false,
                musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: true,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
                batteryEnabled = prefs[Keys.BATTERY_ENABLED] ?: true,
                timerEnabled = prefs[Keys.TIMER_ENABLED] ?: true,
                connectivityEnabled = prefs[Keys.CONNECTIVITY_ENABLED] ?: true,
                callsEnabled = prefs[Keys.CALLS_ENABLED] ?: true,
                startOnBoot = prefs[Keys.START_ON_BOOT] ?: true,
                autoCollapseSec = prefs[Keys.AUTO_COLLAPSE_SEC] ?: 8,
                autoHideWhenIdle = prefs[Keys.AUTO_HIDE_WHEN_IDLE] ?: false,
                hideInLandscape = prefs[Keys.HIDE_IN_LANDSCAPE] ?: true,
                showOnLockScreen = prefs[Keys.SHOW_ON_LOCK_SCREEN] ?: true,
                privacyRedactNotifications = prefs[Keys.PRIVACY_REDACT] ?: true,
                hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK] ?: true
            )
            val next = transform(current)
            prefs[Keys.IS_ENABLED] = next.isEnabled
            prefs[Keys.ONBOARDING_COMPLETED] = next.onboardingCompleted
            prefs[Keys.WIDTH_DP] = next.widthDp
            prefs[Keys.HEIGHT_DP] = next.heightDp
            prefs[Keys.OFFSET_X_DP] = next.offsetXdp
            prefs[Keys.OFFSET_Y_DP] = next.offsetYdp
            prefs[Keys.CORNER_RADIUS_DP] = next.cornerRadiusDp
            prefs[Keys.BG_COLOR_HEX] = next.backgroundColorHex
            prefs[Keys.TRANSPARENCY_ALPHA] = next.transparencyAlpha
            prefs[Keys.TEXT_SIZE_SCALE] = next.textSizeScale
            prefs[Keys.ANIMATION_SPEED_SCALE] = next.animationSpeedScale
            prefs[Keys.NOTIFICATION_DURATION_SEC] = next.notificationDurationSec
            prefs[Keys.THEME_PRESET] = next.themePreset.name
            prefs[Keys.LIQUID_GLASS_ENABLED] = next.liquidGlassEnabled
            prefs[Keys.MUSIC_ENABLED] = next.musicEnabled
            prefs[Keys.NOTIFICATIONS_ENABLED] = next.notificationsEnabled
            prefs[Keys.BATTERY_ENABLED] = next.batteryEnabled
            prefs[Keys.TIMER_ENABLED] = next.timerEnabled
            prefs[Keys.CONNECTIVITY_ENABLED] = next.connectivityEnabled
            prefs[Keys.CALLS_ENABLED] = next.callsEnabled
            prefs[Keys.START_ON_BOOT] = next.startOnBoot
            prefs[Keys.AUTO_COLLAPSE_SEC] = next.autoCollapseSec
            prefs[Keys.AUTO_HIDE_WHEN_IDLE] = next.autoHideWhenIdle
            prefs[Keys.HIDE_IN_LANDSCAPE] = next.hideInLandscape
            prefs[Keys.SHOW_ON_LOCK_SCREEN] = next.showOnLockScreen
            prefs[Keys.PRIVACY_REDACT] = next.privacyRedactNotifications
            prefs[Keys.HAPTIC_FEEDBACK] = next.hapticFeedbackEnabled
        }
    }

    suspend fun applyTheme(theme: IslandTheme) {
        update {
            it.copy(
                themePreset = theme,
                backgroundColorHex = theme.defaultBgColor,
                transparencyAlpha = theme.defaultAlpha,
                liquidGlassEnabled = theme == IslandTheme.LIQUID_GLASS
            )
        }
    }

    suspend fun resetDefaults() {
        update {
            IslandSettings(
                isEnabled = it.isEnabled,
                onboardingCompleted = it.onboardingCompleted
            )
        }
    }
}
