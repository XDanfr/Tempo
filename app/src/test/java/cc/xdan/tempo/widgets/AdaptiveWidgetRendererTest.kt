package cc.xdan.tempo.widgets

import android.app.Activity
import org.robolectric.Robolectric
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import cc.xdan.tempo.R
import cc.xdan.tempo.design.presetColorScheme
import cc.xdan.tempo.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.LEGACY)
class AdaptiveWidgetRendererTest {
    private val now = ZonedDateTime.parse("2026-10-12T10:15:00Z")
    private val table = Timetable(onboarded = true, subjects = listOf(
        Subject("physics", "Physics", icon = SubjectIcon.PHYSICS, colour = 0xFFE5E9A8, location = "Private room"),
        Subject("art", "Art & Design", colour = 0xFFF2C4B2)), sessions = listOf(
        Session("a", "physics", 1, TimeSpan(600, 660)), Session("b", "art", 1, TimeSpan(675, 735)),
        Session("c", "physics", 1, TimeSpan(750, 810)), Session("d", "art", 2, TimeSpan(600, 660))))

    private fun children(view: View): List<View> = listOf(view) + if (view is ViewGroup)
        (0 until view.childCount).flatMap { children(view.getChildAt(it)) } else emptyList()

    @Test fun composesEveryCanonicalSizeWithANativeLabelledCountdownAndPrivacy() = runBlocking {
        val settings = WidgetSettings(showLocation = false, showOutline = false)
        val p = widgetPresentation(table, settings, now)
        for ((w, h) in listOf(140 to 56, 280 to 88, 200 to 220, 360 to 190, 360 to 350, 180 to 420)) {
            val context = RuntimeEnvironment.getApplication().createConfigurationContext(Configuration().apply { densityDpi = 160; fontScale = 1f })
            val root = adaptiveWidgetViews(context, p, WidgetSizing(w.toFloat(), h.toFloat(), 1f), settings,
                presetColorScheme(ThemePreset.FOREST, false), false, true, now.toInstant().toEpochMilli(), preview = true)
                .apply(context, FrameLayout(context))
            Robolectric.buildActivity(Activity::class.java).setup().get().setContentView(root)
            root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            root.layout(0, 0, w, h)
            val views = children(root)
            val texts = views.filterIsInstance<TextView>().map { it.text.toString() }
            assertTrue("$w × $h missing subject: $texts", texts.contains("Physics"))
            assertFalse(texts.any { it.contains("Private room") })
            val timer = views.filterIsInstance<Chronometer>().single()
            assertEquals(R.id.widget_live_time, timer.id)
            assertTrue(timer.isCountDown)
            assertTrue("Empty timer: $texts", timer.text.isNotBlank())
            assertTrue("$w × $h missing label: $texts", texts.any { it == "Time left" || it == "left" })
            assertTrue("$w × $h clipped timer", timer.width > 0 && timer.height > 0)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
            val file = File("build/reports/widget-redesign/${android.os.Build.VERSION.SDK_INT}-current-$w-$h.png")
            file.parentFile!!.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun responsiveStatesStayReadableWithLargeFontsAndLongSubjectNames() = runBlocking {
        val settings = WidgetSettings(showLocation = false, showOutline = false, doneForDayMessage = true)
        val long = table.copy(subjects = table.subjects.map { if (it.id == "physics") it.copy(name = "Computer Science & Engineering") else it },
            breaks = listOf(ScheduledBreak("pause", "Short break", listOf(1), TimeSpan(660, 675))))
        val cases = listOf("upcoming" to now.withHour(9), "break" to now.withHour(11).withMinute(5),
            "done" to now.withHour(18), "week-done" to now.plusDays(5))
        for ((state, time) in cases) for ((w, h) in listOf(140 to 56, 200 to 220, 360 to 190, 360 to 350)) {
            val context = RuntimeEnvironment.getApplication().createConfigurationContext(Configuration().apply { densityDpi = 160; fontScale = 1.5f })
            val p = widgetPresentation(long, settings, time)
            val root = adaptiveWidgetViews(context, p, WidgetSizing(w.toFloat(), h.toFloat(), 1.5f), settings,
                presetColorScheme(ThemePreset.ROSE, true), true, true, time.toInstant().toEpochMilli(), preview = true)
                .apply(context, FrameLayout(context))
            val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
            activity.setContentView(root)
            root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            root.layout(0, 0, w, h)
            val views = children(root)
            assertTrue(views.filterIsInstance<TextView>().any { it.text.toString() == p.title })
            val timer = views.filterIsInstance<Chronometer>().singleOrNull()
            if (state in listOf("upcoming", "break")) {
                assertNotNull(timer)
                assertTrue(timer!!.text.isNotBlank())
                assertEquals(0, timer.layout.getEllipsisCount(0))
                val position = IntArray(2); val origin = IntArray(2)
                timer.getLocationInWindow(position); root.getLocationInWindow(origin)
                val bounds = android.graphics.Rect(position[0] - origin[0], position[1] - origin[1],
                    position[0] - origin[0] + timer.width, position[1] - origin[1] + timer.height)
                assertTrue("$state $w × $h timer outside $bounds", bounds.top >= 0 && bounds.bottom <= h && bounds.left >= 0 && bounds.right <= w)
            } else assertNull(timer)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
            val file = File("build/reports/widget-redesign/${android.os.Build.VERSION.SDK_INT}-$state-$w-$h-large.png")
            file.parentFile!!.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            activity.finish()
        }
    }

    @Test fun terminalStatesHaveNoLiveClockAndDoNotResetSavedSettings() = runBlocking {
        val settings = WidgetSettings(timetableId = "fixed", preset = ThemePreset.ROSE, showLocation = false,
            showIcons = false, themedLessons = true, lessonBackground = true, outerOutline = true, doneForDayMessage = true)
        val context = RuntimeEnvironment.getApplication()
        settings.save(context, 81)
        for (t in listOf(table, table.copy(onboarded = false), table.copy(sessions = emptyList()), null)) {
            val p = widgetPresentation(t, settings, now.withHour(18))
            val root = adaptiveWidgetViews(context, p, WidgetSizing(200f, 220f, 1f), settings,
                presetColorScheme(ThemePreset.ROSE, true), true, false, now.withHour(18).toInstant().toEpochMilli(), preview = true)
                .apply(context, FrameLayout(context))
            assertTrue(children(root).filterIsInstance<Chronometer>().isEmpty())
            assertEquals(settings, WidgetSettings.read(context, 81))
        }
    }
}
