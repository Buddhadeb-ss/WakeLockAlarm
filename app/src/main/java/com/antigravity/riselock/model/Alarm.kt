package com.antigravity.riselock.model

import java.util.Calendar
import java.util.Locale
import java.util.UUID

data class Alarm(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val label: String = "Wake Up",
    val isEnabled: Boolean = true,
    val repeatDays: Set<Int> = emptySet(), // Calendar.MONDAY (2) .. Calendar.SUNDAY (1)
    val missionType: MissionType = MissionType.STEPS,
    val stepTarget: Int = 30, // 20-50 steps default
    val photoReferenceUri: String? = null,
    val photoReferenceHash: Long? = null,
    val photoSpotName: String = "Bathroom Sink",
    val isExtraLoud: Boolean = true,
    val volumePercent: Int = 100
) {
    fun getFormattedTime(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val isAm = cal.get(Calendar.AM_PM) == Calendar.AM
        val h = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val m = String.format(Locale.US, "%02d", minute)
        val amPm = if (isAm) "AM" else "PM"
        return String.format(Locale.US, "%d:%s %s", h, m, amPm)
    }

    fun getDaysFormatted(): String {
        if (repeatDays.isEmpty()) return "Once"
        if (repeatDays.size == 7) return "Every day"
        if (repeatDays == setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)) {
            return "Weekdays"
        }
        if (repeatDays == setOf(Calendar.SATURDAY, Calendar.SUNDAY)) {
            return "Weekends"
        }

        val dayNames = mapOf(
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat",
            Calendar.SUNDAY to "Sun"
        )
        return repeatDays.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
    }

    /**
     * Calculates the exact epoch timestamp in milliseconds for the next trigger.
     */
    fun getNextTriggerMillis(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (repeatDays.isEmpty()) {
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        // Repeating alarm: find the closest scheduled day in the future
        for (i in 0..7) {
            val candidate = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, i)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            if (repeatDays.contains(dayOfWeek) && candidate.timeInMillis > now.timeInMillis) {
                return candidate.timeInMillis
            }
        }

        target.add(Calendar.DAY_OF_YEAR, 7)
        return target.timeInMillis
    }
}

