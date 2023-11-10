package com.lunapos.kioskprinter.singletons

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.annotation.Keep
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lunapos.kioskprinter.Constants.PRINTER_CHANNEL_ID
import com.lunapos.kioskprinter.Constants.PRINTER_NOTIFICATION_ID
import com.lunapos.kioskprinter.MainActivity
import com.lunapos.kioskprinter.R

class Notifications {
    companion object {

        @Volatile
        private var instance: Notifications? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: Notifications().also { instance = it }
            }
    }

    public fun showProgressNotification(context: Context, notificationManager: NotificationManagerCompat,
                                         channelId: String, channelName: String, notificationId: Int,
                                         title: String, content: String,) {

        // Create a NotificationChannel (for Android 8.0 and higher)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.enableLights(true)
            channel.lightColor = Color.BLUE
            notificationManager.createNotificationChannel(channel)
        }

        // Create a NotificationCompat.Builder
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setProgress(100, 0, false) // Initialize progress to 0
            .setOngoing(true) // Set it as ongoing (sticky)

        // Build the notification
        val notification: Notification = builder.build()

        // Display the notification
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManager.notify(notificationId, notification)
    }

    public fun updateProgressNotification(context: Context, notificationManager: NotificationManagerCompat, progress: Int) {
        // Update the progress on the notification
        val notificationBuilder = NotificationCompat.Builder(context, PRINTER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Printer Notification")
            .setContentText("Printer in action")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setProgress(100, progress, false) // Update the progress

        // Build and update the notification
        val updatedNotification: Notification = notificationBuilder.build()
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

        }
        notificationManager.notify(PRINTER_NOTIFICATION_ID, updatedNotification)
    }

    public fun showStickyNotification(context: Context, notificationManager: NotificationManagerCompat,
                                       channelId: String, channelName: String, notificationId: Int,
                                       title: String, message: String) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(false) // Make it sticky
            .setOngoing(true) // Set it as ongoing (sticky)

        // Create an Intent (for example, to open an activity when the notification is clicked)
        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
        notificationBuilder.setContentIntent(intent)

        val notification: Notification = notificationBuilder.build()

        // Display the notification
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManager.notify(notificationId, notification)
    }

    public fun stopStickyNotification(context: Context, notificationManager: NotificationManagerCompat,
                                       channelId: String, notificationId: Int,
                                       title: String, message: String) {

        // Update the notification to remove it from the list of ongoing notifications
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // Remove the ongoing status

        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        val notificationBuild = notification.setContentIntent(intent).build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManager.notify(notificationId, notificationBuild)
    }
}