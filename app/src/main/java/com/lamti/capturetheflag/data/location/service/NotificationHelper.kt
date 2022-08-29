package com.lamti.capturetheflag.data.location.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.lamti.capturetheflag.R
import com.lamti.capturetheflag.presentation.ui.activity.MainActivity

@SuppressLint("UnspecifiedImmutableFlag")
class NotificationHelper(private val context: Context) {

    private val notificationBuilder: NotificationCompat.Builder by lazy {
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setSound(null)
            .setContentIntent(contentIntent)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
    }

    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val contentIntent by lazy {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_MUTABLE
        else
            PendingIntent.FLAG_UPDATE_CURRENT

        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            flag
        )
    }

    fun getNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(createChannel())
        }

        return notificationBuilder.build()
    }

    fun updateNotification(notificationText: String? = null) {
        notificationText?.let { notificationBuilder.setContentText(it) }
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun showEventNotification(
        title: String = "You found opponent's flag",
        content: String = "Tap here to start searching for it with your camera",
        sound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(createHighPriorityChannel(sound))
        }

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_MUTABLE
        else
            PendingIntent.FLAG_UPDATE_CURRENT

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            flag
        )

        val notificationBuilder =
            NotificationCompat.Builder(context, HIGH_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_flag)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(sound)
                .setFullScreenIntent(fullScreenPendingIntent, true)

        notificationManager.notify(HIGH_NOTIFICATION_ID, notificationBuilder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() =
        NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESCRIPTION
            setSound(null, null)
        }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createHighPriorityChannel(sound: Uri) =
        NotificationChannel(
            HIGH_CHANNEL_ID,
            HIGH_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = HIGH_CHANNEL_DESCRIPTION
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)

            val soundAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            setSound(sound, soundAttributes)
        }

    companion object {

        const val NOTIFICATION_ID = 2022
        private const val CHANNEL_ID = "com.capturetheflag.location"
        private const val CHANNEL_NAME = "Capture the flag channel"
        private const val CHANNEL_DESCRIPTION = "This is the CTF - Capture the flag channel"

        private const val HIGH_NOTIFICATION_ID = 2023
        private const val HIGH_CHANNEL_ID = "com.capturetheflag.high"
        private const val HIGH_CHANNEL_NAME = "Capture the flag high priority channel"
        private const val HIGH_CHANNEL_DESCRIPTION = "This is the high priority CTF - Capture the flag channel"
    }

}
