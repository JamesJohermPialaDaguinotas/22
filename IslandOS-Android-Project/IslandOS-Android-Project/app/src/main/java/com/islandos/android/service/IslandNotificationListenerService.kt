package com.islandos.android.service

import android.app.Notification
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.islandos.android.DynamicIslandApp
import com.islandos.android.model.LiveActivity
import java.lang.ref.WeakReference

class IslandNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instanceRef = WeakReference(this)
        val app = applicationContext as? DynamicIslandApp ?: return
        app.coordinator.onNotificationListenerConnected()
        runCatching {
            activeNotifications?.forEach { processPosted(it, initialSync = true) }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instanceRef = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        processPosted(sbn, initialSync = false)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val app = applicationContext as? DynamicIslandApp ?: return
        if (sbn.notification.category == Notification.CATEGORY_CALL) {
            app.coordinator.onCallNotificationRemoved()
        }
        app.coordinator.onNotificationRemoved(sbn.key)
    }

    private fun processPosted(sbn: StatusBarNotification, initialSync: Boolean) {
        if (sbn.packageName == packageName) return
        val notification = sbn.notification ?: return
        val app = applicationContext as? DynamicIslandApp ?: return

        // Always trigger media session re-check when a media notification posts
        if (notification.category == Notification.CATEGORY_TRANSPORT ||
            notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
        ) {
            app.coordinator.refreshMediaSessions()
            return
        }

        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: notification.extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()

        if (notification.category == Notification.CATEGORY_CALL) {
            app.coordinator.onCallNotificationPosted(
                callerTitle = title,
                subtitle = text,
                contentIntent = notification.contentIntent
            )
            return
        }

        // Ignore summary groups and ongoing background noise during regular alerts
        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        if (isGroupSummary || (sbn.isOngoing && initialSync)) return
        if (title.isBlank() && text.isBlank()) return

        val appName = resolveAppName(sbn.packageName)
        val iconBitmap = resolveNotificationIcon(sbn, notification)

        val item = LiveActivity.NotificationActivity(
            id = sbn.key,
            notificationKey = sbn.key,
            packageName = sbn.packageName,
            appName = appName,
            title = title.ifBlank { appName },
            text = text,
            appIcon = iconBitmap,
            timestampMs = sbn.postTime,
            isClearable = sbn.isClearable,
            contentIntent = notification.contentIntent
        )
        app.coordinator.onNotificationPosted(item, burstToIsland = !initialSync)
    }

    private fun resolveAppName(pkg: String): String {
        return runCatching {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(pkg)
    }

    private fun resolveNotificationIcon(sbn: StatusBarNotification, notification: Notification): Bitmap? {
        val smallIconDrawable = runCatching {
            notification.smallIcon?.loadDrawable(this)
        }.getOrNull()

        val appIconDrawable = runCatching {
            packageManager.getApplicationIcon(sbn.packageName)
        }.getOrNull()

        val chosen = appIconDrawable ?: smallIconDrawable ?: return null
        return drawableToBitmap(chosen, 96)
    }

    private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return Bitmap.createScaledBitmap(drawable.bitmap, sizePx, sizePx, true)
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    companion object {
        private var instanceRef: WeakReference<IslandNotificationListenerService>? = null

        fun dismissNotification(key: String) {
            instanceRef?.get()?.let { service ->
                runCatching { service.cancelNotification(key) }
            }
        }
    }
}
