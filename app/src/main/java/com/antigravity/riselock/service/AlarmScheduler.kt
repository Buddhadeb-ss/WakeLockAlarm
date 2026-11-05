package com.antigravity.riselock.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.antigravity.riselock.MainActivity
import com.antigravity.riselock.model.Alarm

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancel(alarm)
            return
        }

        val triggerMillis = alarm.getNextTriggerMillis()
        Log.d("RiseLock", "Scheduling alarm ${alarm.id} for $triggerMillis (${alarm.getFormattedTime()})")

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_LABEL, alarm.label)
            putExtra(EXTRA_MISSION_TYPE, alarm.missionType.name)
            putExtra(EXTRA_STEP_TARGET, alarm.stepTarget)
            putExtra(EXTRA_PHOTO_URI, alarm.photoReferenceUri)
            putExtra(EXTRA_PHOTO_HASH, alarm.photoReferenceHash ?: 0L)
            putExtra(EXTRA_PHOTO_SPOT, alarm.photoSpotName)
            putExtra(EXTRA_EXTRA_LOUD, alarm.isExtraLoud)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // PendingIntent to launch the app if the user taps the alarm icon on lock screen
        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.hashCode() + 1,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
        
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: SecurityException) {
            Log.e("RiseLock", "Failed to schedule exact alarm: missing permission", e)
        }
    }

    fun cancel(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_MISSION_TYPE = "extra_mission_type"
        const val EXTRA_STEP_TARGET = "extra_step_target"
        const val EXTRA_PHOTO_URI = "extra_photo_uri"
        const val EXTRA_PHOTO_HASH = "extra_photo_hash"
        const val EXTRA_PHOTO_SPOT = "extra_photo_spot"
        const val EXTRA_EXTRA_LOUD = "extra_extra_loud"
    }
}

