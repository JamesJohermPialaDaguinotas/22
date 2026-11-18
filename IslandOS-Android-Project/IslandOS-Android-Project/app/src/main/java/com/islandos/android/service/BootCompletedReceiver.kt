package com.islandos.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.islandos.android.DynamicIslandApp
import com.islandos.android.system.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val ctx = context ?: return
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = ctx.applicationContext as? DynamicIslandApp ?: return@launch
                val settings = app.settingsRepository.settingsFlow.first()
                if (settings.isEnabled && settings.startOnBoot && PermissionHelper.hasOverlayPermission(ctx)) {
                    DynamicIslandOverlayService.start(ctx)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
