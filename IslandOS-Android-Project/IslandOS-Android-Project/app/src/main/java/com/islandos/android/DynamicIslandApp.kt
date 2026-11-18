package com.islandos.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.islandos.android.data.SettingsRepository
import com.islandos.android.service.DynamicIslandStateCoordinator

class DynamicIslandApp : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var coordinator: DynamicIslandStateCoordinator
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsRepository = SettingsRepository(applicationContext)
        coordinator = DynamicIslandStateCoordinator(applicationContext, settingsRepository)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val serviceChannel = NotificationChannel(
                CHANNEL_OVERLAY_SERVICE,
                getString(R.string.fgs_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.fgs_channel_description)
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            manager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val CHANNEL_OVERLAY_SERVICE = "island_overlay_fgs"

        @Volatile
        lateinit var instance: DynamicIslandApp
            private set
    }
}
