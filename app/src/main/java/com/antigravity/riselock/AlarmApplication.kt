package com.antigravity.riselock

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class AlarmApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "riselock_foreground_audio_channel",
                "Wake-Up Alarm Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority un-ignorable alarm audio and fullscreen notifications"
                setSound(null, null)
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

