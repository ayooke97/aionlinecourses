package com.aionlinecourses.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aionlinecourses.MainActivity
import com.aionlinecourses.R

class NotificationService(private val context: Context) {
    companion object {
        private const val CHANNEL_ID = "ai_courses_channel"
        private const val DOWNLOAD_CHANNEL_ID = "download_channel"
        
        private const val COURSE_UPDATE_NOTIFICATION_ID = 1
        private const val DOWNLOAD_NOTIFICATION_ID = 2
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Course updates channel
            val courseChannel = NotificationChannel(
                CHANNEL_ID,
                "Course Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for course updates and announcements"
            }
            
            // Download progress channel
            val downloadChannel = NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                "Download Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of course downloads"
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannels(listOf(courseChannel, downloadChannel))
        }
    }
    
    fun showCourseUpdateNotification(
        courseId: Int,
        courseTitle: String,
        message: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("courseId", courseId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(courseTitle)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        with(NotificationManagerCompat.from(context)) {
            notify(COURSE_UPDATE_NOTIFICATION_ID, notification)
        }
    }
    
    fun showDownloadProgressNotification(
        courseId: Int,
        courseTitle: String,
        progress: Int
    ) {
        val notification = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Downloading $courseTitle")
            .setProgress(100, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        
        with(NotificationManagerCompat.from(context)) {
            notify(DOWNLOAD_NOTIFICATION_ID + courseId, notification)
        }
    }
    
    fun showDownloadCompleteNotification(
        courseId: Int,
        courseTitle: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("courseId", courseId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download_complete)
            .setContentTitle("Download Complete")
            .setContentText("$courseTitle is ready for offline viewing")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        with(NotificationManagerCompat.from(context)) {
            // Remove the progress notification
            cancel(DOWNLOAD_NOTIFICATION_ID + courseId)
            // Show the completion notification
            notify(DOWNLOAD_NOTIFICATION_ID + courseId + 1000, notification)
        }
    }
    
    fun cancelDownloadNotification(courseId: Int) {
        with(NotificationManagerCompat.from(context)) {
            cancel(DOWNLOAD_NOTIFICATION_ID + courseId)
        }
    }
}
