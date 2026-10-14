package cc.xdan.tempo.widgets

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.toArgb
import android.view.View
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.TextView
import org.robolectric.RuntimeEnvironment
import cc.xdan.tempo.R
import cc.xdan.tempo.design.presetColorScheme
import cc.xdan.tempo.model.*
import java.io.File
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Local RemoteViews inflation/raster checks, not a launcher or device compatibility test. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CompactWidgetRendererTest {
    private val now = ZonedDateTime.parse("2026-10-12T10:15:00Z")
    private val table = Timetable(onboarded = true, subjects = listOf(Subject("s", "Computer Science", location = "Private room")),
        sessions = listOf(Session("a", "s", 1, TimeSpan(600, 660)), Session("b", "s", 2, TimeSpan(600, 660))))

    @Test fun inflatesAndDrawsCompactStatesAtDifferentBoundsAndFontScales() {
        val base = RuntimeEnvironment.getApplication()
        val settings = WidgetSettings(showLocation = false)
        val cases = listOf("current" to table, "empty" to table.copy(sessions = emptyList()),
            "setup" to table.copy(onboarded = false), "missing" to null)
        for (scale in listOf(1f, 1.5f)) for ((width, height) in listOf(140 to 56, 240 to 72, 320 to 100)) {
            val config = Configuration(base.resources.configuration).apply { fontScale = scale; densityDpi = 160 }
            val context = base.createConfigurationContext(config)
            for ((state, t) in cases) {
                val presentation = widgetPresentation(t, settings, now)
                val remote = compactWidgetViews(context, 12, presentation, WidgetSizing(width.toFloat(), height.toFloat(), scale), settings,
                    presetColorScheme(ThemePreset.FOREST, true), true, true, now.toInstant().toEpochMilli(), preview = true)
                val root = remote.apply(context, FrameLayout(context))
                root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                root.layout(0, 0, width, height)
                assertEquals(presentation.title, root.findViewById<TextView>(R.id.widget_title).text.toString())
                assertFalse(root.findViewById<TextView>(R.id.widget_detail).text.contains("Private room"))
                assertEquals(if (state == "current") View.VISIBLE else View.GONE, root.findViewById<Chronometer>(R.id.widget_timer).visibility)
                if (state == "current") {
                    val timer = root.findViewById<Chronometer>(R.id.widget_timer)
                    val label = root.findViewById<TextView>(R.id.widget_countdown_label)
                    assertTrue(label.text.toString() in listOf("Left", "Time left"))
                    assertEquals(View.VISIBLE, label.visibility)
                    assertEquals(1, timer.lineCount)
                    assertEquals(0, timer.layout.getEllipsisCount(0))
                }
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                root.draw(Canvas(bitmap))
                val output = File("build/reports/widget-previews/${android.os.Build.VERSION.SDK_INT}-$state-$width-$height-$scale.png")
                output.parentFile!!.mkdirs()
                output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                assertTrue(output.length() > 100)
            }
        }
    }

    @Test fun upcomingBreakAndCompletionUseTruthfulLabels() {
        val context = RuntimeEnvironment.getApplication()
        val lunch = table.copy(sessions = table.sessions + Session("c", "s", 1, TimeSpan(720, 780)),
            breaks = listOf(ScheduledBreak("lunch", "Lunch", listOf(1), TimeSpan(660, 690))))
        val cases = listOf(
            Triple(table, now.withHour(9), "In"),
            Triple(lunch, now.withHour(11), "Left"),
            Triple(table, now.withHour(12), ""))
        for ((t, time, label) in cases) {
            val settings = WidgetSettings(doneForDayMessage = true)
            val presentation = widgetPresentation(t, settings, time)
            val root = compactWidgetViews(context, 5, presentation, WidgetSizing(140f, 56f, 1f), settings,
                presetColorScheme(ThemePreset.FOREST, true), true, true, time.toInstant().toEpochMilli(), preview = true).apply(context, FrameLayout(context))
            val actual = root.findViewById<TextView>(R.id.widget_countdown_label)
            if (label.isEmpty()) assertEquals(View.GONE, actual.visibility) else assertEquals(label, actual.text.toString())
            assertEquals(presentation.title, root.findViewById<TextView>(R.id.widget_title).text.toString())
        }
    }

    @Test fun compactResizeRetainsThemedLessonColourPreference() {
        val context = RuntimeEnvironment.getApplication()
        val redSubject = table.copy(subjects = listOf(table.subjects.single().copy(colour = 0xFFFF0000)))
        val settings = WidgetSettings(themedLessons = true, showOutline = false)
        val scheme = presetColorScheme(ThemePreset.FOREST, true)
        val presentation = widgetPresentation(redSubject, settings, now)
        val root = compactWidgetViews(context, 8, presentation, WidgetSizing(320f, 100f, 1f), settings,
            scheme, true, false, now.toInstant().toEpochMilli(), preview = true).apply(context, FrameLayout(context))
        root.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, 320, 100)
        val bitmap = Bitmap.createBitmap(320, 100, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val expected = widgetLessonBackground(scheme, androidx.compose.ui.graphics.Color.Red, true, true, true).toArgb()
        assertEquals(expected, bitmap.getPixel(220, 90))
        assertNotEquals(android.graphics.Color.RED, bitmap.getPixel(220, 90))
    }

    @Test fun persistentSettingsSurviveRerenderAndCompletionHidesTimer() {
        val context = RuntimeEnvironment.getApplication()
        val settings = WidgetSettings(timetableId = "fixed", preset = ThemePreset.ROSE, showLocation = false, doneForDayMessage = true, showOutline = false)
        settings.save(context, 42)
        val completed = widgetPresentation(table, WidgetSettings.read(context, 42), now.withHour(12))
        val root = compactWidgetViews(context, 42, completed, WidgetSizing(280f, 96f, 1f), settings,
            presetColorScheme(ThemePreset.ROSE, false), false, true, now.withHour(12).toInstant().toEpochMilli(), preview = true).apply(context, FrameLayout(context))
        assertEquals("Done for today", root.findViewById<TextView>(R.id.widget_title).text.toString())
        assertEquals(View.GONE, root.findViewById<Chronometer>(R.id.widget_timer).visibility)
        assertEquals(settings, WidgetSettings.read(context, 42))
    }
}
