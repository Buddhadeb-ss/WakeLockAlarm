package com.antigravity.riselock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.antigravity.riselock.R
import com.antigravity.riselock.ui.screens.RingingActivity

class AlarmAudioService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var previousVolume: Int = -1

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val label = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: "Wake Up Alarm"
        val isExtraLoud = intent?.getBooleanExtra(AlarmScheduler.EXTRA_EXTRA_LOUD, true) ?: true

        if (action == ACTION_STOP_ALARM) {
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        }

        // Start Foreground Notification
        createNotificationChannel()
        val notification = buildForegroundNotification(label)
        startForeground(NOTIFICATION_ID, notification)

        startAlarmSound(isExtraLoud)
        startVibration()

        return START_STICKY
    }

    private fun startAlarmSound(isExtraLoud: Boolean) {
        if (mediaPlayer != null && mediaPlayer?.isPlaying == true) return

        try {
            audioManager?.let { am ->
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                previousVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
                // Force max volume if extra loud
                if (isExtraLoud) {
                    am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
                }
            }

            var alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri!!)
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build()
                setAudioAttributes(audioAttributes)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("RiseLock", "Error starting alarm audio player", e)
        }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 800, 300, 800, 300, 1000)
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 0) // 0 means repeat indefinitely
                v.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, 0)
            }
        }
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null

            vibrator?.cancel()

            // Restore previous volume if changed
            if (previousVolume != -1) {
                audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, previousVolume, 0)
            }
        } catch (e: Exception) {
            Log.e("RiseLock", "Error stopping alarm audio", e)
        }
    }

    private fun buildForegroundNotification(label: String): Notification {
        val fullScreenIntent = Intent(this, RingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("RiseLock: $label")
            .setContentText("Mission in progress. Wake up verification required.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RiseLock Alarm Sound",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active un-ignorable alarm audio loop"
                setSound(null, null) // Custom sound handled by MediaPlayer
                enableVibration(false) // Custom vibration handled directly
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_ALARM = "com.antigravity.riselock.ACTION_START"
        const val ACTION_STOP_ALARM = "com.antigravity.riselock.ACTION_STOP"
        const val CHANNEL_ID = "riselock_foreground_audio_channel"
        const val NOTIFICATION_ID = 2001
    }
}

