package com.antigravity.riselock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.antigravity.riselock.data.AlarmRepository
import com.antigravity.riselock.ui.screens.RingingActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("RiseLock", "Alarm triggered! Waking up device...")

        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: ""
        val alarmLabel = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: "Wake Up"
        val missionType = intent.getStringExtra(AlarmScheduler.EXTRA_MISSION_TYPE) ?: "STEPS"
        val stepTarget = intent.getIntExtra(AlarmScheduler.EXTRA_STEP_TARGET, 30)
        val photoUri = intent.getStringExtra(AlarmScheduler.EXTRA_PHOTO_URI)
        val photoHash = intent.getLongExtra(AlarmScheduler.EXTRA_PHOTO_HASH, 0L)
        val photoSpot = intent.getStringExtra(AlarmScheduler.EXTRA_PHOTO_SPOT) ?: "Bathroom Sink"
        val isExtraLoud = intent.getBooleanExtra(AlarmScheduler.EXTRA_EXTRA_LOUD, true)

        // 1. Acquire temporary screen wake lock to ensure display turns on
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "RiseLock:AlarmWakeLock"
        )
        wakeLock.acquire(10 * 60 * 1000L) // 10 minutes max safety limit

        // 2. Start high-priority un-ignorable foreground audio service
        val serviceIntent = Intent(context, AlarmAudioService::class.java).apply {
            action = AlarmAudioService.ACTION_START_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, alarmLabel)
            putExtra(AlarmScheduler.EXTRA_EXTRA_LOUD, isExtraLoud)
        }
        context.startForegroundService(serviceIntent)

        // 3. Launch full-screen ringing activity over lock screen
        val ringIntent = Intent(context, RingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, alarmLabel)
            putExtra(AlarmScheduler.EXTRA_MISSION_TYPE, missionType)
            putExtra(AlarmScheduler.EXTRA_STEP_TARGET, stepTarget)
            putExtra(AlarmScheduler.EXTRA_PHOTO_URI, photoUri)
            putExtra(AlarmScheduler.EXTRA_PHOTO_HASH, photoHash)
            putExtra(AlarmScheduler.EXTRA_PHOTO_SPOT, photoSpot)
        }
        context.startActivity(ringIntent)

        // 4. Reschedule if repeating alarm
        if (alarmId.isNotEmpty()) {
            val repository = AlarmRepository(context)
            val alarm = repository.getAlarmById(alarmId)
            if (alarm != null) {
                if (alarm.repeatDays.isNotEmpty()) {
                    AlarmScheduler(context).schedule(alarm)
                } else {
                    repository.toggleAlarm(alarmId, false)
                }
            }
        }
    }
}

