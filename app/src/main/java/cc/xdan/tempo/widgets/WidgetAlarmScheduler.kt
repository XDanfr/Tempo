package cc.xdan.tempo.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build

/** One non-wakeup alarm for the earliest label or schedule change across all widgets. */
internal object WidgetAlarmScheduler {
    fun hasAccurateTiming(context: Context): Boolean = Build.VERSION.SDK_INT < 31 ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun schedule(context: Context, at: Long, refresh: PendingIntent) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        if (hasAccurateTiming(context)) {
            try {
                alarm.setExact(AlarmManager.RTC, at, refresh)
                return
            } catch (_: SecurityException) {
                // Access can be revoked between checking it and scheduling the alarm.
            }
        }
        alarm.setAndAllowWhileIdle(AlarmManager.RTC, at, refresh)
    }
}
