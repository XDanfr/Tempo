package cc.xdan.tempo.widgets

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import cc.xdan.tempo.model.ThemePreset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 35])
class WidgetRefreshTest {
    @Test fun interactiveRefreshOnlyRendersRequestedWidgetAndPreservesGlobalAlarmAndSettings() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val manager = shadowOf(AppWidgetManager.getInstance(context))
        for (id in listOf(301, 302)) {
            manager.addBoundWidget(id, AppWidgetProviderInfo().apply {
                provider = ComponentName(context, AdaptiveTempoWidget::class.java)
            })
            WidgetSettings(preset = ThemePreset.FOREST, showLocation = false).save(context, id)
        }
        val before = WidgetSettings.read(context, 301)
        val alarm = shadowOf(context.getSystemService(AlarmManager::class.java))
        val scheduled = alarm.scheduledAlarms.toList()
        val other = manager.getViewFor(302)
        refreshWidgets(context, intArrayOf(301), schedule = false)
        assertNotNull(manager.getViewFor(301))
        assertSame(other, manager.getViewFor(302))
        assertEquals(scheduled, alarm.scheduledAlarms.toList())
        assertEquals(before, WidgetSettings.read(context, 301))
    }
}
