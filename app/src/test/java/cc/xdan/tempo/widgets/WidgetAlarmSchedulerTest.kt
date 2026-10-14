package cc.xdan.tempo.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 35])
class WidgetAlarmSchedulerTest {
    @Test fun timingAccessControlsAccuracyWithoutWakeupsOrAdditionalAlarms() {
        val context = RuntimeEnvironment.getApplication()
        val shadow = shadowOf(context.getSystemService(AlarmManager::class.java))
        val refresh = PendingIntent.getBroadcast(context, 0, Intent(context, WidgetClockReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        for (allowed in listOf(false, true, false)) {
            ShadowAlarmManager.setCanScheduleExactAlarms(allowed)
            val precise = allowed || Build.VERSION.SDK_INT < 31
            assertEquals(precise, WidgetAlarmScheduler.hasAccurateTiming(context))
            WidgetAlarmScheduler.schedule(context, 60_000, refresh)
            val alarm = shadow.scheduledAlarms.single()
            assertEquals(AlarmManager.RTC, alarm.type)
            assertEquals(60_000L, alarm.triggerAtTime)
            assertEquals(precise, alarm.windowLengthMs == 0L)
            assertEquals(!precise, alarm.isAllowWhileIdle)
        }
    }
}
