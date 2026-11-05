package com.antigravity.riselock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.antigravity.riselock.data.AlarmRepository

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED) {

            Log.d("RiseLock", "Boot or time change detected. Rescheduling all active alarms...")
            val repository = AlarmRepository(context)
            val scheduler = AlarmScheduler(context)

            val alarms = repository.getAlarms()
            for (alarm in alarms) {
                if (alarm.isEnabled) {
                    scheduler.schedule(alarm)
                }
            }
        }
    }
}

