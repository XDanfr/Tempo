package cc.xdan.tempo.widgets

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import cc.xdan.tempo.MainActivity
import cc.xdan.tempo.R
import cc.xdan.tempo.schedule.ScheduleBlock
import cc.xdan.tempo.model.minuteLabel
import cc.xdan.tempo.ui.accent
import cc.xdan.tempo.ui.symbol
import cc.xdan.tempo.ui.title

/** Snapshot authoring keeps the existing provider IDs and boundary-based refresh lifecycle. */
internal suspend fun adaptiveWidgetViews(context: Context, presentation: WidgetPresentation,
    sizing: WidgetSizing, settings: WidgetSettings, scheme: ColorScheme, dark: Boolean,
    showIcons: Boolean, nowEpochMillis: Long, preview: Boolean = false): RemoteViews =
    AdaptiveWidgetSnapshot(presentation, sizing, settings, scheme, dark, showIcons, nowEpochMillis, preview)
        .compose(context, size = DpSize(sizing.widthDp.dp, sizing.heightDp.dp))

private class AdaptiveWidgetSnapshot(
    val p: WidgetPresentation, val size: WidgetSizing, val settings: WidgetSettings,
    val scheme: ColorScheme, val dark: Boolean, val icons: Boolean, val now: Long, val preview: Boolean,
) : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetContent(context) }
    }

    private val plan = WidgetLayoutPlan(size)
    private val ticking = p.targetEpochMillis?.minus(now)?.let { it in 1..604_800_000 } == true
    private val accent = p.block?.accent(scheme.primaryFixed) ?: scheme.primaryFixed
    private val eventColour = widgetLessonBackground(scheme, accent, dark, settings.themedLessons, true)
    private val heroColour = if (settings.lessonBackground && ticking) eventColour else scheme.primaryContainer
    private val timerColour = if (settings.lessonBackground) scheme.secondaryContainer else eventColour
    private val unit get() = size.fontScale.coerceAtLeast(1f)
    private val status get() = when (p.state) {
        WidgetState.CURRENT -> "Now"
        WidgetState.BREAK -> "Break"
        WidgetState.UPCOMING -> if (p.dayComplete) "Next day" else "Up next"
        WidgetState.DONE_TODAY -> "Day complete"
        WidgetState.WEEK_DONE -> "Week complete"
        WidgetState.MISSING, WidgetState.SETUP -> "Let’s get started"
        WidgetState.EMPTY -> "A little breathing room"
    }
    private val detail get() = when (p.state) {
        WidgetState.MISSING -> "Choose a timetable in widget settings"
        WidgetState.SETUP -> "Open Tempo to finish setup"
        WidgetState.WEEK_DONE -> "Nothing else scheduled this week"
        WidgetState.DONE_TODAY -> "${p.summary?.next?.lesson?.title().orEmpty()} · ${p.summary?.next?.starts?.dayOfWeek?.name?.lowercase()?.replaceFirstChar { it.titlecase() }.orEmpty()}"
        WidgetState.EMPTY -> "Your week is clear"
        else -> p.block?.let { "${minuteLabel(it.time.start)}–${minuteLabel(it.time.end)}" }.orEmpty()
    }

    @Composable private fun WidgetContent(context: Context) {
        val action = if (preview) GlanceModifier else GlanceModifier.clickable(actionStartActivity(
            Intent(context, MainActivity::class.java).putExtra("widgetTimetableId", settings.timetableId)))
        val modifier = GlanceModifier.fillMaxSize().appWidgetBackground().then(action)
        when (plan.composition) {
            WidgetComposition.COMPACT -> Compact(context, modifier)
            WidgetComposition.SQUARE -> Square(context, modifier)
            WidgetComposition.WIDE -> Wide(context, modifier)
            WidgetComposition.EXPANDED -> Expanded(context, modifier)
        }
    }

    @Composable private fun Label(value: String, ink: Color, sp: Int = 12, bold: Boolean = false,
        lines: Int = 1, modifier: GlanceModifier = GlanceModifier) {
        Text(value, modifier, style = TextStyle(color = ColorProvider(ink), fontSize = sp.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal), maxLines = lines)
    }

    @Composable private fun Icon(ink: Color, dp: Int = 24, completed: Boolean = p.state in listOf(WidgetState.DONE_TODAY, WidgetState.WEEK_DONE)) {
        val vector = if (completed) Icons.Outlined.Check else if (p.block != null) p.block?.symbol() else Icons.Outlined.CalendarMonth
        if (icons && vector != null) Image(ImageProvider(widgetIcon(vector, ink.toArgb())), null,
            GlanceModifier.size((dp * unit).dp))
    }

    @Composable private fun Timer(context: Context, ink: Color, width: Float, height: Float, sp: Float) {
        if (ticking) AndroidRemoteViews(nativeWidgetCountdown(context, p.targetEpochMillis!!, now, ink,
            width * unit, height * unit, sp), GlanceModifier.width((width * unit).dp).height((height * unit).dp))
    }

    @Composable private fun Surface(context: Context, colour: Color, modifier: GlanceModifier,
        edge: Int = 0, outline: Boolean = settings.showOutline, root: Boolean = false, content: @Composable () -> Unit) {
        val fills = listOf(R.drawable.widget_compact_single, R.drawable.widget_compact_start,
            R.drawable.widget_compact_end, R.drawable.widget_group_top, R.drawable.widget_group_middle, R.drawable.widget_group_bottom)
        val borders = listOf(R.drawable.widget_compact_single_outline, R.drawable.widget_compact_start_outline,
            R.drawable.widget_compact_end_outline, R.drawable.widget_group_top_outline, R.drawable.widget_group_middle_outline, R.drawable.widget_group_bottom_outline)
        val rtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val side = if (rtl && edge in 1..2) 3 - edge else edge
        Box(modifier) {
            Image(ImageProvider(if (root) R.drawable.widget_outer_surface else fills[side]), null,
                GlanceModifier.fillMaxSize(), contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(ColorProvider(colour)))
            if (outline) Image(ImageProvider(if (root) R.drawable.widget_outer_outline else borders[side]), null, GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds, colorFilter = ColorFilter.tint(ColorProvider(scheme.primary)))
            content()
        }
    }

    @Composable private fun Compact(context: Context, modifier: GlanceModifier) {
        val ink = widgetForeground(heroColour)
        val height = size.height.toFloat()
        val inset = if (height < 56) 8 else 12
        if (plan.compactSplit && ticking) {
            Row(modifier) {
                Surface(context, heroColour, GlanceModifier.defaultWeight().fillMaxHeight(), edge = 1) {
                    Column(GlanceModifier.fillMaxSize().padding(inset.dp), verticalAlignment = Alignment.Vertical.CenterVertically) {
                        Label(status, ink, 11)
                        Label(p.title, ink, 18, true, if (height >= 76) 2 else 1)
                        if (height >= 100 && p.location != null) Label(p.location, ink)
                    }
                }
                Spacer(GlanceModifier.width(WidgetShapeRules.GAP.dp))
                val width = (size.width / 2f - 18).coerceAtLeast(76f)
                Surface(context, timerColour, GlanceModifier.defaultWeight().fillMaxHeight(), edge = 2) {
                    Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                        verticalAlignment = Alignment.Vertical.CenterVertically) {
                        Timer(context, widgetForeground(timerColour), width, height - 28, 32f)
                        Label(p.countdownLabel, widgetForeground(timerColour), 11)
                    }
                }
            }
        } else Surface(context, heroColour, modifier, root = true) {
            Column(GlanceModifier.fillMaxSize().padding(horizontal = inset.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically) {
                Label(p.title, ink, if (height < 48) 12 else if (height >= 68) 18 else 14, true, if (!ticking && height >= 68) 2 else 1)
                if (ticking) Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    if (!p.current) { Label("In", ink, 11); Spacer(GlanceModifier.width(4.dp)) }
                    Timer(context, ink, (size.width - inset * 2 - 32).toFloat().coerceAtLeast(50f),
                        (height - 30).coerceIn(14f, 36f), if (height >= 68) 28f else 20f)
                    Spacer(GlanceModifier.width(4.dp))
                    if (p.current) Label("left", ink, 11)
                } else if (height >= 90) Label(detail, ink, 11)
            }
        }
    }

    @Composable private fun Square(context: Context, modifier: GlanceModifier) {
        val ink = scheme.onSurface
        Surface(context, scheme.surfaceContainer, modifier, outline = settings.outerOutline, root = true) {
            Column(GlanceModifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically) {
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    if (icons && size.height >= 185 && p.block?.symbol() != null) {
                        Icon(scheme.primary, 16); Spacer(GlanceModifier.width(4.dp))
                    }
                    Label(status, scheme.primary, 11, true)
                }
                Spacer(GlanceModifier.height(4.dp))
                Label(p.title, ink, if (size.height < 185) 18 else 22, true, if (size.height >= 185) 2 else 1)
                Spacer(GlanceModifier.height(8.dp))
                if (ticking) {
                    val badge = plan.badgeSize.toFloat()
                    Box(GlanceModifier.size((badge * unit).dp), contentAlignment = Alignment.Center) {
                        Image(ImageProvider(widgetCookie((badge * unit * context.resources.displayMetrics.density).toInt(), timerColour)),
                            null, GlanceModifier.fillMaxSize())
                        Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                            Timer(context, widgetForeground(timerColour), badge * .76f, badge * .34f, 36f)
                            if (badge >= 88) Label(p.countdownLabel, widgetForeground(timerColour), 11)
                        }
                    }
                    if (badge < 88) {
                        Spacer(GlanceModifier.height(4.dp))
                        Label(p.countdownLabel, ink, 11)
                    }
                    if (plan.squareContext) {
                        Spacer(GlanceModifier.height(8.dp))
                        Label(listOfNotNull(detail, p.location).joinToString(" · "), ink, 12)
                    }
                } else {
                    if (icons && size.height >= 185) {
                        Box(GlanceModifier.size(64.dp), contentAlignment = Alignment.Center) {
                            Image(ImageProvider(widgetCookie((64 * context.resources.displayMetrics.density).toInt(), scheme.primaryContainer)), null, GlanceModifier.fillMaxSize())
                            Icon(scheme.onPrimaryContainer, 28)
                        }
                        Spacer(GlanceModifier.height(8.dp))
                    }
                    Label(detail, ink, 12, lines = 2)
                }
            }
        }
    }

    @Composable private fun Hero(context: Context, modifier: GlanceModifier, height: Int) {
        val ink = widgetForeground(heroColour)
        val timerWidth = ((size.width - 36) * .43f).coerceAtLeast(74f)
        Row(modifier) {
            Surface(context, heroColour, GlanceModifier.defaultWeight().fillMaxHeight(), edge = if (ticking) 1 else 0) {
                Column(GlanceModifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                        if (icons && height >= 130 && size.width >= 310 && p.block?.symbol() != null) {
                            Icon(ink, 20); Spacer(GlanceModifier.width(6.dp))
                        }
                        Label(status, ink, 12, true)
                    }
                    Spacer(GlanceModifier.height(6.dp))
                    Label(p.title, ink, if (size.width >= 350) 26 else 22, true, 2)
                    if (height >= 130) {
                        Spacer(GlanceModifier.height(6.dp))
                        Label(if (ticking) listOfNotNull(p.location, detail).joinToString(" · ") else detail, ink, 12, lines = if (ticking) 1 else 2)
                    }
                    if (p.progress != null && height >= 160) {
                        Spacer(GlanceModifier.height(12.dp))
                        LinearProgressIndicator(p.progress, GlanceModifier.fillMaxWidth().height(4.dp), ColorProvider(ink), ColorProvider(ink.copy(alpha = .15f)))
                    }
                }
            }
            if (ticking) {
                Spacer(GlanceModifier.width(WidgetShapeRules.GAP.dp))
                Surface(context, timerColour, GlanceModifier.width((timerWidth * unit).dp).fillMaxHeight(), edge = 2) {
                    Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                        verticalAlignment = Alignment.Vertical.CenterVertically) {
                        Timer(context, widgetForeground(timerColour), timerWidth - 24, 64f.coerceAtMost(height - 32f), 44f)
                        Label(p.countdownLabel, widgetForeground(timerColour), 12, true)
                    }
                }
            }
        }
    }

    @Composable private fun Wide(context: Context, modifier: GlanceModifier) {
        Surface(context, scheme.surfaceContainer, modifier, outline = settings.outerOutline, root = true) {
            Column(GlanceModifier.fillMaxSize().padding(8.dp)) {
                Hero(context, GlanceModifier.fillMaxWidth().defaultWeight(), size.height - if (plan.wideContext) 60 else 16)
                if (plan.wideContext) {
                    val next = p.plan?.later?.firstOrNull()
                    Row(GlanceModifier.fillMaxWidth().height(44.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.Vertical.CenterVertically) {
                        Label(if (next != null) "Next" else if (p.plan != null) "Finish" else "Tempo", scheme.primary, 11, true)
                        Spacer(GlanceModifier.width(8.dp))
                        Label(next?.let { "${minuteLabel(it.time.start)} · ${it.title()}" } ?: p.plan?.let { minuteLabel(it.finish) } ?: detail,
                            scheme.onSurface, 12, modifier = GlanceModifier.defaultWeight())
                    }
                }
            }
        }
    }

    @Composable private fun Expanded(context: Context, modifier: GlanceModifier) {
        val later = p.plan?.later.orEmpty()
        if (later.isEmpty()) { Wide(context, modifier); return }
        val heroHeight = plan.heroHeight + (plan.agendaRows - later.size).coerceAtLeast(0) * 56
        Surface(context, scheme.surfaceContainer, modifier, outline = settings.outerOutline, root = true) {
            Column(GlanceModifier.fillMaxSize().padding(8.dp)) {
                if (size.stacked) {
                    // Tall narrow footprints stay a single entity; avoid squeezing two horizontal panels.
                    Surface(context, heroColour, GlanceModifier.fillMaxWidth().height((heroHeight * unit).dp)) {
                        Column(GlanceModifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.Vertical.CenterVertically) {
                            Label(status, widgetForeground(heroColour), 11, true)
                            Label(p.title, widgetForeground(heroColour), 20, true, 2)
                            if (ticking) {
                                Timer(context, widgetForeground(heroColour), (size.width - 48).toFloat(), 40f, 30f)
                                Label(p.countdownLabel, widgetForeground(heroColour), 11)
                            }
                        }
                    }
                } else Hero(context, GlanceModifier.fillMaxWidth().height((heroHeight * unit).dp), heroHeight)
                val rows = later.take(plan.agendaRows)
                Row(GlanceModifier.fillMaxWidth().height(32.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Label(if (p.plan != null) "Finish ${minuteLabel(p.plan.finish)}" else status, scheme.primary, 11, true,
                        modifier = GlanceModifier.defaultWeight())
                    val hidden = later.size - rows.size
                    if (hidden > 0) Label("+$hidden more", scheme.onSurface, 11)
                }
                rows.forEachIndexed { index, block ->
                    val fill = widgetLessonBackground(scheme, block.accent(scheme.primaryFixed), dark, settings.themedLessons, false)
                    val edge = if (rows.size == 1) 0 else if (index == 0) 3 else if (index == rows.lastIndex) 5 else 4
                    Surface(context, fill, GlanceModifier.fillMaxWidth().height((52 * unit).dp), edge) {
                        Row(GlanceModifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.Vertical.CenterVertically) {
                            Label(minuteLabel(block.time.start), widgetForeground(fill), 12, true)
                            Spacer(GlanceModifier.width(12.dp))
                            Label(block.title(), widgetForeground(fill), 14, true, modifier = GlanceModifier.defaultWeight())
                        }
                    }
                    if (index != rows.lastIndex) Spacer(GlanceModifier.height(WidgetShapeRules.GAP.dp))
                }
                if (rows.isEmpty()) Label(if (ticking) "Your last event for this day" else detail, scheme.onSurface, 12,
                    lines = 2, modifier = GlanceModifier.padding(12.dp))
            }
        }
    }
}
