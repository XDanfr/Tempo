package cc.xdan.tempo.widgets

import android.content.Intent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 35])
class WidgetClockObserverTest {
    @Test fun clockAndUnlockSignalsAreRegisteredOnceAndRemovedWithLastWidget() {
        val context = RuntimeEnvironment.getApplication()
        val app = shadowOf(context)
        // Installed apps receive their own merged AndroidX signature permission.
        // Robolectric's API26 permission model needs that install-time grant explicitly.
        app.grantPermissions("${context.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        WidgetClockObserver.update(context, true)
        WidgetClockObserver.update(context, true)
        for (action in listOf(Intent.ACTION_TIME_TICK, Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT)) {
            assertEquals(1, app.getReceiversForIntent(Intent(action)).filterIsInstance<WidgetClockReceiver>().size)
        }
        WidgetClockObserver.update(context, false)
        for (action in listOf(Intent.ACTION_TIME_TICK, Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT)) {
            assertTrue(app.getReceiversForIntent(Intent(action)).filterIsInstance<WidgetClockReceiver>().isEmpty())
        }
    }

    @Test fun minuteBroadcastsOnlyRenderWhenTheVisibleLabelOrScheduleNeedsUpdating() {
        WidgetClockObserver.refreshedUntil(120_000)
        assertFalse(WidgetClockObserver.isDue(60_000))
        assertTrue(WidgetClockObserver.isDue(120_000))
        assertTrue(WidgetClockObserver.isDue(180_000))
        WidgetClockObserver.update(RuntimeEnvironment.getApplication(), false)
        assertFalse(WidgetClockObserver.isDue(180_000))
    }
}
