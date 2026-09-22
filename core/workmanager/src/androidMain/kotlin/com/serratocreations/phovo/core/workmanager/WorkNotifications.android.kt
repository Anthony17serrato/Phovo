package com.serratocreations.phovo.core.workmanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.ForegroundInfo

/**
 * Builds the ongoing notification behind long-running work.
 *
 * Deliberately uses a platform system icon rather than shipping a drawable, so the module needs no
 * resources of its own. Swap [ICON] for an app drawable when the notification is worth branding.
 */
internal class WorkNotifications(private val context: Context) {

    private val manager = context.getSystemService(NotificationManager::class.java)

    fun foregroundInfo(
        title: String,
        subtitle: String,
        completed: Long,
        total: Long
    ): ForegroundInfo {
        ensureChannel()

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        builder.setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(ICON)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (total > 0) {
            builder.setProgress(total.toInt(), completed.coerceAtMost(total).toInt(), false)
        } else {
            // Nothing has reported a total yet, so show an indeterminate bar rather than 0%.
            builder.setProgress(0, 0, true)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, builder.build())
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        )
    }

    private companion object {
        const val CHANNEL_ID = "phovo.work.longrunning"
        const val CHANNEL_NAME = "Background sync"
        const val NOTIFICATION_ID = 0x50484F56 // "PHOV"
        val ICON = android.R.drawable.stat_sys_upload
    }
}
