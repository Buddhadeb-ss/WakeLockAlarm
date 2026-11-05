package com.antigravity.riselock.data

import android.content.Context
import android.content.SharedPreferences
import com.antigravity.riselock.model.Alarm
import com.antigravity.riselock.model.MissionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class AlarmRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("riselock_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val alarmsKey = "saved_alarms_v1"

    private val _alarmsFlow = MutableStateFlow<List<Alarm>>(emptyList())
    val alarmsFlow: StateFlow<List<Alarm>> = _alarmsFlow.asStateFlow()

    init {
        loadAlarms()
    }

    private fun loadAlarms() {
        val json = prefs.getString(alarmsKey, null)
        val list: List<Alarm> = if (json != null) {
            val type = object : TypeToken<List<Alarm>>() {}.type
            try {
                gson.fromJson(json, type) ?: getDefaultAlarms()
            } catch (e: Exception) {
                getDefaultAlarms()
            }
        } else {
            getDefaultAlarms()
        }
        _alarmsFlow.value = list
    }

    private fun saveToPrefs(list: List<Alarm>) {
        _alarmsFlow.value = list
        val json = gson.toJson(list)
        prefs.edit().putString(alarmsKey, json).apply()
    }

    fun getAlarms(): List<Alarm> = _alarmsFlow.value

    fun getAlarmById(id: String): Alarm? = _alarmsFlow.value.find { it.id == id }

    fun addOrUpdateAlarm(alarm: Alarm) {
        val current = _alarmsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == alarm.id }
        if (index != -1) {
            current[index] = alarm
        } else {
            current.add(alarm)
        }
        current.sortBy { it.hour * 60 + it.minute }
        saveToPrefs(current)
    }

    fun toggleAlarm(id: String, enabled: Boolean) {
        val current = _alarmsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(isEnabled = enabled)
            saveToPrefs(current)
        }
    }

    fun deleteAlarm(id: String) {
        val current = _alarmsFlow.value.filterNot { it.id == id }
        saveToPrefs(current)
    }

    private fun getDefaultAlarms(): List<Alarm> {
        return listOf(
            Alarm(
                hour = 7,
                minute = 0,
                label = "Morning Wake Up",
                isEnabled = true,
                repeatDays = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
                missionType = MissionType.STEPS,
                stepTarget = 30,
                isExtraLoud = true
            ),
            Alarm(
                hour = 8,
                minute = 30,
                label = "Weekend Fresh Start",
                isEnabled = false,
                repeatDays = setOf(Calendar.SATURDAY, Calendar.SUNDAY),
                missionType = MissionType.PHOTO_MATCH,
                photoSpotName = "Bathroom Sink",
                isExtraLoud = true
            )
        )
    }
}

