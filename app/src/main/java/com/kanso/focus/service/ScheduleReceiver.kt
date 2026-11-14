package com.kanso.focus.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kanso.focus.data.FocusSessionManager

class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        FocusSessionManager.init(context)
        val duration = intent?.getIntExtra("duration_minutes", 45) ?: 45
        val strict = intent?.getBooleanExtra("strict_mode", true) ?: true
        val hour = intent?.getIntExtra("hour", FocusSessionManager.scheduleHour.value)
            ?: FocusSessionManager.scheduleHour.value
        val minute = intent?.getIntExtra("minute", FocusSessionManager.scheduleMinute.value)
            ?: FocusSessionManager.scheduleMinute.value

        if (strict) {
            FocusSessionManager.startStrictTimedSession(duration)
        } else {
            FocusSessionManager.startFlexibleSession(duration)
        }

        if (FocusSessionManager.scheduleEnabled.value) {
            FocusSessionManager.scheduleDailySession(
                context = context,
                hourOfDay = hour,
                minute = minute,
                durationMinutes = duration,
                strictMode = strict
            )
        }
    }
}
