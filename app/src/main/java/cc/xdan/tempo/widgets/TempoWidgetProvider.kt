package cc.xdan.tempo.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import cc.xdan.tempo.ui.accent
import cc.xdan.tempo.ui.symbol
import cc.xdan.tempo.ui.title
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.work.*
import cc.xdan.tempo.MainActivity
import cc.xdan.tempo.R
import cc.xdan.tempo.data.TimetableRepository
import cc.xdan.tempo.design.presetColorScheme
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.*
import kotlinx.coroutines.flow.first
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

open class TempoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { WidgetRefresh.enqueue(context) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) { WidgetRefresh.enqueue(context) }
    override fun onEnabled(context: Context) { WidgetRefresh.start(context) }
    override fun onDeleted(context: Context, ids: IntArray) { ids.forEach { WidgetSettings.delete(context, it) }; WidgetRefresh.enqueue(context) }
    override fun onDisabled(context: Context) { WidgetRefresh.enqueue(context) }
}
class PillWidget : TempoWidgetProvider()
class SquareWidget : TempoWidgetProvider()
class WideWidget : TempoWidgetProvider()
class WidgetClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { WidgetRefresh.start(context) }
}

object WidgetRefresh {
    fun ids(context: Context): IntArray {
        val manager = AppWidgetManager.getInstance(context)
        return listOf(PillWidget::class.java, SquareWidget::class.java, WideWidget::class.java)
            .flatMap { manager.getAppWidgetIds(ComponentName(context, it)).toList() }.toIntArray()
    }
    fun start(context: Context) {
        if (ids(context).isEmpty()) return
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("tempo-widgets-periodic", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES).build())
        enqueue(context)
    }
    fun enqueue(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork("tempo-widgets-refresh", ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build())
    }
}

class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val manager = AppWidgetManager.getInstance(context)
        val ids = WidgetRefresh.ids(context)
        val alarm = context.getSystemService(AlarmManager::class.java)
        val refresh = PendingIntent.getBroadcast(context, 0, Intent(context, WidgetClockReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (ids.isEmpty()) {
            WorkManager.getInstance(context).cancelUniqueWork("tempo-widgets-periodic")
            alarm.cancel(refresh)
            return Result.success()
        }
        return try {
            val collection = TimetableRepository(context).collection.first()
            val now = ZonedDateTime.now()
            var nextRefresh = now.plusDays(1).toInstant().toEpochMilli()
            ids.forEach { id ->
                val settings = WidgetSettings.read(context, id)
                val timetable = if (settings.timetableId == null) collection.active else collection.timetables.firstOrNull { it.id == settings.timetableId }?.timetable
                val provider = manager.getAppWidgetInfo(id)?.provider?.className
                val pill = provider == PillWidget::class.java.name
                val wide = provider == WideWidget::class.java.name
                val appearance = timetable?.appearance ?: Appearance()
                val selected = settings.preset ?: appearance.selectedPreset
                val mode = if (settings.preset == null) appearance.mode else settings.mode
                val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && (context.resources.configuration.uiMode and 0x30) == 0x20)
                val scheme = if (selected == ThemePreset.MATERIAL_YOU && android.os.Build.VERSION.SDK_INT >= 31) {
                    if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else presetColorScheme(selected, dark)
                val options = manager.getAppWidgetOptions(id)
                val height = (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 90) / context.resources.configuration.fontScale).toInt().coerceIn(56, 400)
                val summary = timetable?.takeIf { it.onboarded }?.let { weekOverview(it, now.toLocalDateTime()) }
                summary?.boundary?.atZone(now.zone)?.let { nextRefresh = minOf(nextRefresh, it.toInstant().toEpochMilli()) }
                val dayComplete = timetable != null && summary != null && widgetDayComplete(timetable, now.toLocalDateTime(), summary)
                val completion = !pill && dayComplete
                val weekDone = summary?.status == WeekStatus.DONE
                val pillDone = pill && (weekDone || dayComplete && settings.doneForDayMessage)
                val messageOnly = completion && !wide && settings.doneForDayMessage
                val plan = if (wide && !completion && timetable != null && summary != null) widgetDayPlan(timetable, now.toLocalDateTime(), summary) else null
                val agendaLayout = plan?.later?.isNotEmpty() == true
                val block = summary?.block
                val lessonPanel = wide && !completion && block != null
                val views = RemoteViews(context.packageName, when {
                    pillDone -> R.layout.tempo_widget_pill_done
                    pill -> R.layout.tempo_widget_pill
                    completion && wide -> R.layout.tempo_widget_done_today_wide
                    completion -> R.layout.tempo_widget_done_today
                    agendaLayout -> R.layout.tempo_widget_wide
                    lessonPanel -> R.layout.tempo_widget_wide_single
                    else -> R.layout.tempo_widget
                })
                val accent = block?.accent(scheme.primaryFixed) ?: scheme.primaryFixed
                val themeBackground = widgetThemeBackground(scheme, dark)
                val background = if (!wide && !completion && !pillDone && block != null && settings.lessonBackground) accent else themeBackground
                val lessonFill = widgetLessonBackground(scheme, accent, dark, wide && settings.themedLessons, true)
                val foreground = widgetForeground(if (lessonPanel) lessonFill else background)
                val current = summary?.status == WeekStatus.CURRENT
                val lesson = block as? ScheduleBlock.Lesson
                val target = if (current && block != null) now.toLocalDate().atStartOfDay().plusMinutes(block.time.end.toLong()).atZone(now.zone)
                    else summary?.next?.starts?.atZone(now.zone)
                val until = target?.toInstant()?.toEpochMilli()?.minus(now.toInstant().toEpochMilli())
                val ticking = !messageOnly && until != null && until in 1..604_800_000
                val showIcon = settings.showIcons ?: appearance.showIcons
                val completionBadge = widgetCompletionBadge(scheme, dark)
                fun text(id: Int, value: String, colour: Color, size: Float, lines: Int = 2) {
                    views.setTextViewText(id, value)
                    views.setTextColor(id, colour.toArgb())
                    views.setTextViewTextSize(id, android.util.TypedValue.COMPLEX_UNIT_SP, size)
                    views.setInt(id, "setMaxLines", lines)
                }
                fun badge(frame: Int, fill: Int, icon: Int, symbol: androidx.compose.ui.graphics.vector.ImageVector, colour: Color, visible: Boolean) {
                    views.setInt(fill, "setColorFilter", colour.toArgb())
                    views.setImageViewBitmap(icon, widgetIcon(symbol, widgetForeground(colour).toArgb()))
                    views.setViewVisibility(frame, if (visible) View.VISIBLE else View.GONE)
                }
                fun timer(id: Int, colour: Color, size: Float) {
                    views.setTextColor(id, colour.toArgb())
                    views.setTextViewTextSize(id, android.util.TypedValue.COMPLEX_UNIT_SP, size)
                    views.setViewVisibility(id, if (ticking) View.VISIBLE else View.GONE)
                    views.setChronometerCountDown(id, true)
                    views.setChronometer(id, SystemClock.elapsedRealtime() + (until ?: 0), if (current) "%s left" else "In %s", ticking)
                }
                fun surface(target: RemoteViews, fillId: Int, outlineId: Int, fill: Color, border: Boolean) {
                    val root = fillId == R.id.widget_background
                    val shape = if (root) { if (pill) R.drawable.widget_shape_pill else R.drawable.widget_shape_surface } else R.drawable.widget_shape_block
                    val edge = if (root) { if (pill) R.drawable.widget_shape_pill_outline else R.drawable.widget_shape_surface_outline } else R.drawable.widget_shape_block_outline
                    target.setImageViewResource(fillId, shape)
                    target.setImageViewResource(outlineId, edge)
                    target.setInt(fillId, "setColorFilter", fill.toArgb())
                    target.setInt(outlineId, "setColorFilter", widgetOutline(scheme).toArgb())
                    target.setViewVisibility(outlineId, if (border) View.VISIBLE else View.GONE)
                }
                surface(views, R.id.widget_background, R.id.widget_outline, background, settings.showOutline && (!wide || !completion && !weekDone))
                when {
                    pillDone -> {
                        badge(R.id.widget_badge, R.id.widget_badge_background, R.id.widget_icon, Icons.Outlined.CheckCircle, completionBadge, showIcon)
                        text(R.id.widget_title, if (weekDone) "All done" else "Done for today", foreground, if (weekDone) 22f else 16f, 1)
                    }
                    completion -> {
                        val ink = widgetForeground(themeBackground)
                        badge(R.id.widget_badge, R.id.widget_badge_background, R.id.widget_icon, Icons.Outlined.CheckCircle, completionBadge,
                            showIcon && (messageOnly || height >= if (wide) 190 else 300))
                        text(R.id.widget_title, "Done for today!", ink, if (wide) { if (height < 190) 26f else 30f } else if (height < 200) 22f else 24f)
                        views.setViewVisibility(R.id.widget_next_pill, if (messageOnly) View.GONE else View.VISIBLE)
                        val next = summary?.next?.lesson
                        val nextColour = widgetLessonBackground(scheme, next?.accent(scheme.primaryFixed) ?: scheme.primaryFixed, dark, wide && settings.themedLessons, false)
                        val nextInk = widgetForeground(nextColour)
                        surface(views, R.id.widget_next_pill_background, R.id.widget_next_pill_outline, nextColour, wide && settings.showOutline)
                        val nextBadge = lerp(nextColour, Color.White, .30f)
                        badge(R.id.widget_next_badge, R.id.widget_next_badge_background, R.id.widget_next_icon,
                            next?.symbol() ?: Icons.Outlined.CalendarMonth, nextBadge, showIcon && height >= if (wide) 190 else 250)
                        text(R.id.widget_next_title, next?.title().orEmpty(), nextInk, if (wide) 24f else if (height < 200) 18f else 20f, if (!wide && height < 200) 1 else 2)
                        timer(R.id.widget_next_timer, nextInk, if (wide) 20f else 16f)
                    }
                    else -> {
                        if (lessonPanel) surface(views, R.id.widget_block_background, R.id.widget_block_outline, lessonFill, settings.showOutline)
                        val title = when {
                            timetable == null -> "Choose a timetable"
                            !timetable.onboarded -> "Finish setup"
                            weekDone -> "All done"
                            summary?.status == WeekStatus.EMPTY -> settings.emptyText
                            else -> block?.title() ?: settings.emptyText
                        }
                        val primaryDate = if (current) now.toLocalDate() else summary?.next?.starts?.toLocalDate()
                        val whenText = block?.let {
                            val day = if (primaryDate != now.toLocalDate()) primaryDate?.format(DateTimeFormatter.ofPattern("EEE"))?.plus(" · ") ?: "" else ""
                            "$day${minuteLabel(it.time.start)}–${minuteLabel(it.time.end)}"
                        }.orEmpty()
                        val location = if (settings.showLocation) lesson?.location?.takeIf { it.isNotBlank() } else null
                        val detail = when {
                            timetable == null -> "Choose another timetable in widget settings"
                            !timetable.onboarded -> "Open Tempo to finish setup"
                            weekDone -> "Nothing else scheduled this week"
                            summary?.status == WeekStatus.EMPTY -> "Your week is clear"
                            else -> whenText
                        }
                        val icon = when {
                            weekDone -> Icons.Outlined.CheckCircle
                            summary?.status == WeekStatus.EMPTY -> Icons.Outlined.EventAvailable
                            else -> block?.symbol() ?: Icons.Outlined.CalendarMonth
                        }
                        val showBadge = showIcon && (if (weekDone) height >= 150 else height >= 240)
                        if (!pill) badge(R.id.widget_badge, R.id.widget_badge_background, R.id.widget_icon, icon,
                            if (weekDone || block == null) completionBadge else lerp(if (lessonPanel) lessonFill else accent, Color.White, .30f), showBadge)
                        else {
                            views.setViewVisibility(R.id.widget_icon, if (showIcon) View.VISIBLE else View.GONE)
                            views.setImageViewBitmap(R.id.widget_icon, widgetIcon(icon, foreground.toArgb()))
                        }
                        val roomPill = !pill && location != null && (height >= 280 || !wide && !showBadge && height >= 150)
                        text(R.id.widget_title, title, foreground, when {
                            pill -> 14f
                            weekDone -> if (height < 180) 28f else if (wide) 36f else 32f
                            wide && !agendaLayout -> 28f
                            height < 150 -> 18f
                            height < 200 -> 20f
                            else -> 24f
                        }, if (pill || !weekDone && height < 150) 1 else 2)
                        text(R.id.widget_detail, if (location != null && !roomPill) listOf(whenText, location).joinToString(" · ") else detail,
                            foreground, if (pill) 11f else if (height < 180) 13f else 16f, if (pill || height < 200 && !weekDone) 1 else 2)
                        timer(R.id.widget_timer, foreground, if (pill) 12f else if (height < 150) 20f else 26f)
                        views.setViewVisibility(R.id.widget_detail, if (pill && ticking) View.GONE else View.VISIBLE)
                        if (!pill) {
                            val roomColour = lerp(if (lessonPanel) lessonFill else accent, Color.White, .30f)
                            views.setInt(R.id.widget_location_background, "setColorFilter", roomColour.toArgb())
                            text(R.id.widget_location_text, location.orEmpty(), widgetForeground(roomColour), 13f, 1)
                            views.setViewVisibility(R.id.widget_location, if (roomPill) View.VISIBLE else View.GONE)
                        }
                        if (agendaLayout && plan != null) {
                            views.removeAllViews(R.id.widget_agenda)
                            val later = plan.later.take(if (height >= 190) 2 else 1)
                            val hidden = plan.later.size - later.size
                            text(R.id.widget_day_summary, "Finish ${minuteLabel(plan.finish)} · ${plan.lessonsLeft} left", widgetForeground(themeBackground), 12f, 1)
                            later.forEachIndexed { index, activity ->
                                val row = RemoteViews(context.packageName, R.layout.tempo_widget_agenda_row)
                                val colour = widgetLessonBackground(scheme, activity.accent(scheme.primaryFixed), dark, settings.themedLessons, false)
                                surface(row, R.id.widget_row_background, R.id.widget_row_outline, colour, settings.showOutline)
                                row.setTextViewText(R.id.widget_row_title, activity.title())
                                val room = if (settings.showLocation) (activity as? ScheduleBlock.Lesson)?.location?.takeIf { it.isNotBlank() } else null
                                val overflow = if (index == later.lastIndex && hidden > 0) "+$hidden more" else room
                                row.setTextViewText(R.id.widget_row_detail, listOfNotNull("${minuteLabel(activity.time.start)}–${minuteLabel(activity.time.end)}", overflow).joinToString(" · "))
                                listOf(R.id.widget_row_title, R.id.widget_row_detail).forEach { row.setTextColor(it, widgetForeground(colour).toArgb()) }
                                views.addView(R.id.widget_agenda, row)
                            }
                        }
                    }
                }
                val open = Intent(context, MainActivity::class.java).putExtra("widgetTimetableId", settings.timetableId)
                views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                manager.updateAppWidget(id, views)
            }
            alarm.setAndAllowWhileIdle(AlarmManager.RTC, nextRefresh.coerceAtLeast(System.currentTimeMillis() + 1000), refresh)
            Result.success()
        } catch (_: Exception) { Result.retry() }
    }
}
