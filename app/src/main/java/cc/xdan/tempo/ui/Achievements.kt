package cc.xdan.tempo.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.xdan.tempo.design.TempoMotion
import cc.xdan.tempo.design.rememberTempoHaptics
import cc.xdan.tempo.model.*
import cc.xdan.tempo.schedule.Progress
import cc.xdan.tempo.schedule.sharedFrees
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Permanent discovery achievements. They reward finding useful parts of Tempo, never coming back
 * daily, and never unlock or gate anything. Stored only on this device.
 */
enum class Achievement(val title: String, val detail: String, val icon: ImageVector) {
    FIRST_TIMETABLE("Your week, in view", "Finish setting up a timetable", Icons.Outlined.CalendarToday),
    FIRST_SESSION("Off to a start", "Add your first session", Icons.Outlined.AddTask),
    FULL_LIBRARY("Well stocked", "Have five subjects or activities", Icons.Outlined.CollectionsBookmark),
    MAKE_IT_YOURS("Make it yours", "Pick a theme or mode other than the default", Icons.Outlined.Palette),
    WEEK_AT_A_GLANCE("Week at a glance", "Open the Week tab", Icons.Outlined.ViewWeek),
    SECOND_THOUGHTS("Second thoughts", "Undo a change", Icons.Outlined.Undo),
    TIME_OFF("Time off", "Add a holiday or a day off", Icons.Outlined.BeachAccess),
    IN_ROTATION("In rotation", "Set up alternating weeks", Icons.Outlined.Autorenew),
    ON_THE_HOME_SCREEN("On the home screen", "Add a Tempo widget", Icons.Outlined.Widgets),
    SAFE_KEEPING("Safe keeping", "Export a backup of a timetable", Icons.Outlined.SaveAlt),
    SHARE_THE_LOVE("Share the love", "Share a timetable link", Icons.Outlined.Link),
    FRIEND_ADDED("Better together", "Add a friend's timetable", Icons.Outlined.Group),
    FREE_TOGETHER("Free together", "Have a free that lines up with a friend's", Icons.Outlined.Diversity3),
    FIRST_CHECK_IN("Present", "Check in to a session", Icons.Outlined.TaskAlt),
    ON_A_ROLL("On a roll", "Reach a 5-day streak", Icons.Outlined.LocalFireDepartment),
    LEVEL_THREE("Levelling up", "Reach level 3", Icons.Outlined.MilitaryTech),
}

object Achievements {
    private fun prefs(context: Context) = context.getSharedPreferences("achievements", Context.MODE_PRIVATE)

    fun unlocked(context: Context): Map<Achievement, Long> = Achievement.entries.mapNotNull { a ->
        prefs(context).getLong(a.name, 0L).takeIf { it > 0 }?.let { a to it }
    }.toMap()

    /** Records an action-based achievement (visiting a tab, undoing, exporting). */
    fun mark(context: Context, achievement: Achievement) {
        prefs(context).edit().putBoolean("event:${achievement.name}", true).apply()
    }

    private fun event(context: Context, achievement: Achievement) = prefs(context).getBoolean("event:${achievement.name}", false)

    private fun widgetPlaced(context: Context): Boolean = runCatching {
        val manager = AppWidgetManager.getInstance(context)
        listOf("AdaptiveTempoWidget", "PillWidget", "SquareWidget", "WideWidget").any { name ->
            manager.getAppWidgetIds(ComponentName(context, "cc.xdan.tempo.widgets.$name")).isNotEmpty()
        }
    }.getOrDefault(false)

    /** Which achievements the current state earns. */
    fun earned(context: Context, collection: TimetableCollection, hasOwnedShare: Boolean): Set<Achievement> {
        val mine = collection.timetables.map { it.timetable }.filter { it.person == null }
        val active = collection.active
        val people = collection.timetables.filter { it.id != collection.activeId && it.timetable.person != null }
            .map { it.timetable.person!! to it.timetable }
        val today = LocalDate.now()
        val summary = if (active.progress == ProgressMode.STREAKS) Progress.summary(active, today) else null
        return buildSet {
            if (mine.any { it.onboarded }) add(Achievement.FIRST_TIMETABLE)
            if (mine.any { it.sessions.isNotEmpty() }) add(Achievement.FIRST_SESSION)
            if (mine.any { it.subjects.size >= 5 }) add(Achievement.FULL_LIBRARY)
            if (mine.any { it.appearance.selectedPreset != ThemePreset.FOREST || it.appearance.mode != ThemeMode.SYSTEM }) add(Achievement.MAKE_IT_YOURS)
            if (mine.any { it.holidays.isNotEmpty() || it.exceptions.any { e -> e.closed } }) add(Achievement.TIME_OFF)
            if (mine.any { it.cycle.weeks > 1 }) add(Achievement.IN_ROTATION)
            if (hasOwnedShare) add(Achievement.SHARE_THE_LOVE)
            if (people.isNotEmpty()) add(Achievement.FRIEND_ADDED)
            if (people.isNotEmpty() && (0 until 7).any { sharedFrees(active, people, today.with(DayOfWeek.MONDAY).plusDays(it.toLong())).isNotEmpty() }) add(Achievement.FREE_TOGETHER)
            if (mine.any { it.checkIns.isNotEmpty() }) add(Achievement.FIRST_CHECK_IN)
            if (summary != null && summary.bestStreak >= 5) add(Achievement.ON_A_ROLL)
            if (summary != null && summary.level >= 3) add(Achievement.LEVEL_THREE)
            if (widgetPlaced(context)) add(Achievement.ON_THE_HOME_SCREEN)
            listOf(Achievement.WEEK_AT_A_GLANCE, Achievement.SECOND_THOUGHTS, Achievement.SAFE_KEEPING).filter { event(context, it) }.forEach(::add)
        }
    }

    /** Saves newly earned achievements and returns them, oldest definition first. */
    fun unlockNew(context: Context, earned: Set<Achievement>): List<Achievement> {
        val known = unlocked(context).keys
        val fresh = earned.filter { it !in known }.sortedBy { it.ordinal }
        if (fresh.isNotEmpty()) prefs(context).edit().apply { fresh.forEach { putLong(it.name, System.currentTimeMillis()) } }.apply()
        return fresh
    }
}

/** A badge in a Material shape; locked badges are muted with a lock. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AchievementBadge(achievement: Achievement, unlocked: Boolean, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 56.dp) {
    val shapes = listOf(MaterialShapes.Cookie9Sided, MaterialShapes.Clover4Leaf, MaterialShapes.Sunny, MaterialShapes.Cookie6Sided, MaterialShapes.Gem, MaterialShapes.Flower)
    val shape = shapes[achievement.ordinal % shapes.size].toShape()
    val colour = if (unlocked) personColour(achievement.name) else MaterialTheme.colorScheme.surfaceContainerHighest
    Box(modifier.size(size).clip(shape).background(colour), contentAlignment = Alignment.Center) {
        Icon(if (unlocked) achievement.icon else Icons.Outlined.Lock, null, Modifier.size(size * .42f),
            tint = if (unlocked) cc.xdan.tempo.design.subjectForeground(colour) else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Rounded notice that drops in from the top when an achievement unlocks, one at a time. Quiet
 * (no notice) when celebrations are off; the achievement still appears on the profile.
 */
@Composable
fun AchievementNotices(queue: List<Achievement>, consumed: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = rememberTempoHaptics()
    val current = queue.firstOrNull()
    var visible by remember(current) { mutableStateOf(false) }
    LaunchedEffect(current) {
        if (current == null) return@LaunchedEffect
        delay(400); visible = true; haptics.celebrate()
        delay(3_600); visible = false
        delay(450); consumed()
    }
    AnimatedVisibility(visible && current != null, modifier = modifier,
        enter = slideInVertically(TempoMotion.slide) { -it * 2 } + fadeIn(TempoMotion.effects()) + scaleIn(TempoMotion.fastSpatial(), initialScale = .8f),
        exit = slideOutVertically(TempoMotion.slide) { -it * 2 } + fadeOut(TempoMotion.exit())) {
        val shown = current ?: return@AnimatedVisibility
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shadowElevation = 6.dp, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
            Box {
            // A brief fall of dark Material shapes behind the text.
            val base = personColour(shown.name)
            FallingConfetti(listOf(base, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                .map { androidx.compose.ui.graphics.lerp(it, androidx.compose.ui.graphics.Color.Black, .45f).copy(alpha = .55f) }, Modifier.matchParentSize())
            Row(Modifier.padding(start = 8.dp, end = 22.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AchievementBadge(shown, true, size = 44.dp)
                Column {
                    Text("Achievement unlocked", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.inversePrimary)
                    Text(shown.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            }
        }
    }
}

/** Tracks the collection and surfaces newly unlocked achievements. */
@Composable
fun rememberAchievementQueue(collection: TimetableCollection?, celebrate: Boolean): Pair<List<Achievement>, () -> Unit> {
    val context = LocalContext.current
    var queue by remember { mutableStateOf(emptyList<Achievement>()) }
    var firstRun by remember { mutableStateOf(true) }
    var events by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val prefs = context.getSharedPreferences("achievements", Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if (key?.startsWith("event:") == true) events++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    LaunchedEffect(collection, events) {
        val current = collection ?: return@LaunchedEffect
        val earned = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            Achievements.earned(context, current, cc.xdan.tempo.share.ShareStore.allOwned(context).isNotEmpty())
        }
        val fresh = Achievements.unlockNew(context, earned)
        // Achievements already earned before this version are recorded quietly, not announced all at once.
        val alreadyHad = firstRun && context.getSharedPreferences("achievements", Context.MODE_PRIVATE).getBoolean("seeded", false).not()
        if (firstRun) context.getSharedPreferences("achievements", Context.MODE_PRIVATE).edit().putBoolean("seeded", true).apply()
        firstRun = false
        if (celebrate && !alreadyHad) queue = queue + fresh
    }
    return queue to { queue = queue.drop(1) }
}

/**
 * A short (about 1.5 s) fall of Material Expressive shapes, clipped to its container and drawn
 * behind content. Skipped when system animations are off.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FallingConfetti(colours: List<androidx.compose.ui.graphics.Color>, modifier: Modifier = Modifier, durationMillis: Int = 1_500) {
    val context = LocalContext.current
    val animationsOn = remember { android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
    if (!animationsOn) return
    val shapes = listOf(MaterialShapes.Cookie4Sided, MaterialShapes.Clover4Leaf, MaterialShapes.Sunny, MaterialShapes.Pill, MaterialShapes.Circle, MaterialShapes.Gem).map { it.toShape() }
    val pieces = remember {
        val random = kotlin.random.Random(System.nanoTime())
        List(18) { listOf(random.nextFloat(), random.nextFloat() * .6f, 6f + random.nextFloat() * 6f, (random.nextFloat() - .5f) * 540f, random.nextFloat() * .35f,
            random.nextInt(shapes.size).toFloat(), random.nextInt(colours.size).toFloat(), (random.nextFloat() - .5f) * .2f) }
    }
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, androidx.compose.animation.core.tween(durationMillis, easing = androidx.compose.animation.core.LinearEasing)) }
    val layout = androidx.compose.ui.platform.LocalLayoutDirection.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.foundation.Canvas(modifier) {
        val t = progress.value
        if (t <= 0f || t >= 1f) return@Canvas
        // Clip to the pill in the drawing itself, so pieces never spill outside the notice.
        val pill = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, androidx.compose.ui.geometry.CornerRadius(size.height / 2)))
        }
        clipPath(pill) {
        pieces.forEach { (x, startY, sizeDp, spin, delay, shapeIndex, colourIndex, drift) ->
            val local = ((t - delay) / (1f - delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val side = sizeDp.dp.toPx()
            val px = size.width * (x + drift * local)
            val py = -side + (size.height * (1.3f + startY)) * local * local + size.height * startY * (1 - local) - size.height * startY
            val outline = shapes[shapeIndex.toInt()].createOutline(androidx.compose.ui.geometry.Size(side, side), layout, density)
            drawContext.canvas.save()
            drawContext.transform.translate(px - side / 2, py)
            drawContext.transform.rotate(spin * local, androidx.compose.ui.geometry.Offset(side / 2, side / 2))
            drawOutline(outline, colours[colourIndex.toInt()], alpha = if (local > .75f) (1f - local) / .25f else 1f)
            drawContext.canvas.restore()
        }
        }
    }
}

private operator fun <T> List<T>.component6() = this[5]
private operator fun <T> List<T>.component7() = this[6]
private operator fun <T> List<T>.component8() = this[7]
