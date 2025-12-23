package alie.info.newmultichoice.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object NotificationPermissionHelper {
    
    private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    
    fun hasNotificationPermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For Android 12 and below, notifications are allowed by default
            true
        }
    }
    
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(activity)) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }
    
    fun scheduleStreakReminder(activity: Activity) {
        if (hasNotificationPermission(activity)) {
            // Schedule daily reminder using WorkManager
            androidx.work.PeriodicWorkRequestBuilder<alie.info.newmultichoice.notifications.DailyReminderWorker>(
                24, java.util.concurrent.TimeUnit.HOURS
            ).build().also { workRequest ->
                androidx.work.WorkManager.getInstance(activity)
                    .enqueueUniquePeriodicWork(
                        "daily_streak_reminder",
                        androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                        workRequest
                    )
            }
        } else {
            requestNotificationPermission(activity)
        }
    }
}

