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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
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
    /** XP each achievement adds, app-wide. */
    const val XP = 25
    fun xp(context: Context): Int = unlocked(context).size * XP

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
        val summary = if (collection.progressMode == ProgressMode.STREAKS && active.person == null) Progress.summary(active.copy(bonusXp = xp(context)), today) else null
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

/** Something worth a notice at the top of the screen. */
sealed interface Celebration {
    /** [xpAfter] is your total XP once it's added, or null when XP is off. */
    data class Unlocked(val achievement: Achievement, val xpAfter: Int? = null) : Celebration
    data class LevelUp(val level: Int) : Celebration
    data class XpGain(val xpBefore: Int, val xpAfter: Int) : Celebration
}

/** Level, XP into it, and its goal for a total. */
private fun levelOf(total: Int): Triple<Int, Int, Int> {
    var level = 1; var remaining = total
    while (remaining >= Progress.levelGoal(level)) { remaining -= Progress.levelGoal(level); level++ }
    return Triple(level, remaining, Progress.levelGoal(level))
}

/** XP bar that fills from [from] to [to] with an ease-in, and how much is left to the next level. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun XpFill(from: Int, to: Int, modifier: Modifier = Modifier) {
    val (level, into, goal) = levelOf(to)
    val start = if (levelOf(from).first == level) levelOf(from).second else 0
    val fill = remember(from, to) { Animatable(start.toFloat() / goal) }
    LaunchedEffect(from, to) { delay(150); fill.animateTo(into.toFloat() / goal, tween(900, easing = EaseIn)) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("+${to - from} XP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Level $level", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        LinearWavyProgressIndicator(progress = { fill.value }, modifier = Modifier.fillMaxWidth(), waveSpeed = 0.dp)
        Text("${goal - into} XP to level ${level + 1}", style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Rounded notice that drops in from the top when an achievement unlocks or you level up, one at a time.
 * Swipe it up to dismiss early. Quiet (no notice) when celebrations are off.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AchievementNotices(queue: List<Celebration>, consumed: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = rememberTempoHaptics()
    val current = queue.firstOrNull()
    var visible by remember(current) { mutableStateOf(false) }
    var dismissed by remember(current) { mutableStateOf(false) }
    val drag = remember(current) { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(current) {
        if (current == null) return@LaunchedEffect
        delay(400); visible = true; if (current is Celebration.XpGain) haptics.confirm() else haptics.celebrate()
        if (current is Celebration.LevelUp) { delay(180); haptics.celebrate() }
        // Long enough for the slowest confetti to finish falling, unless swiped away.
        kotlinx.coroutines.withTimeoutOrNull(when (current) { is Celebration.LevelUp -> 5_600L; is Celebration.XpGain -> 2_600L; else -> 4_600L }) { snapshotFlow { dismissed }.first { it } }
        visible = false
        delay(450); consumed()
    }
    AnimatedVisibility(visible && current != null, modifier = modifier,
        enter = slideInVertically(TempoMotion.slide) { -it * 2 } + fadeIn(TempoMotion.effects()) + scaleIn(TempoMotion.fastSpatial(), initialScale = .8f),
        exit = slideOutVertically(TempoMotion.slide) { -it * 2 } + fadeOut(TempoMotion.exit())) {
        val shown = current ?: return@AnimatedVisibility
        val scheme = MaterialTheme.colorScheme
        val confetti = listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.inversePrimary, scheme.tertiaryContainer, scheme.secondaryContainer)
        val swipe = Modifier
            .graphicsLayer { translationY = drag.value; alpha = 1f - (-drag.value / 400f).coerceIn(0f, .6f) }
            .pointerInput(shown) {
                detectVerticalDragGestures(
                    onDragEnd = { scope.launch { if (drag.value < -60f) dismissed = true else drag.animateTo(0f, TempoMotion.fastSpatial()) } },
                    onDragCancel = { scope.launch { drag.animateTo(0f, TempoMotion.fastSpatial()) } },
                ) { change, amount -> change.consume(); scope.launch { drag.snapTo((drag.value + amount).coerceAtMost(0f)) } }
            }
            .semantics { liveRegion = LiveRegionMode.Polite; customActions = listOf(CustomAccessibilityAction("Dismiss") { dismissed = true; true }) }
        when (shown) {
            is Celebration.XpGain -> Surface(shape = RoundedCornerShape(28.dp), color = scheme.primaryContainer, contentColor = scheme.onPrimaryContainer,
                shadowElevation = 10.dp, border = androidx.compose.foundation.BorderStroke(1.5.dp, scheme.primary.copy(alpha = .6f)), modifier = swipe.width(280.dp)) {
                XpFill(shown.xpBefore, shown.xpAfter, Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            }
            is Celebration.Unlocked -> Surface(shape = RoundedCornerShape(36.dp), color = scheme.primaryContainer, contentColor = scheme.onPrimaryContainer,
                shadowElevation = 10.dp, border = androidx.compose.foundation.BorderStroke(1.5.dp, scheme.primary.copy(alpha = .6f)), modifier = swipe.widthIn(min = 280.dp)) {
                // The achievement first, then it fades into the XP it added.
                var showXp by remember(shown) { mutableStateOf(false) }
                LaunchedEffect(shown) { if (shown.xpAfter != null) { delay(2_000); showXp = true } }
                Box(Modifier.animateContentSize(TempoMotion.spatial())) {
                    FallingConfetti(confetti, Modifier.matchParentSize(), corner = 36.dp)
                    Crossfade(showXp, label = "achievement xp") { xp -> if (xp && shown.xpAfter != null)
                        XpFill(shown.xpAfter - Achievements.XP, shown.xpAfter, Modifier.width(260.dp).padding(horizontal = 20.dp, vertical = 12.dp))
                    else Row(Modifier.padding(start = 8.dp, end = 22.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AchievementBadge(shown.achievement, true, size = 44.dp)
                        Column {
                            Text("Achievement unlocked · +${Achievements.XP} XP", style = MaterialTheme.typography.labelMedium, color = scheme.primary)
                            Text(shown.achievement.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    } }
                }
            }
            // Bigger and louder: a card with a spinning, pulsing level badge and a denser, longer fall.
            is Celebration.LevelUp -> Surface(shape = RoundedCornerShape(32.dp), color = scheme.tertiaryContainer, contentColor = scheme.onTertiaryContainer,
                shadowElevation = 14.dp, border = androidx.compose.foundation.BorderStroke(2.dp, scheme.tertiary.copy(alpha = .7f)), modifier = swipe.padding(horizontal = 24.dp)) {
                Box {
                    FallingConfetti(confetti, Modifier.matchParentSize(), durationMillis = 3_400, count = 80, corner = 32.dp)
                    val spin = rememberInfiniteTransition(label = "level spin")
                    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(6_000, easing = LinearEasing)), label = "angle")
                    val pulse by spin.animateFloat(.92f, 1.08f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse")
                    Column(Modifier.padding(horizontal = 32.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }) {
                            Box(Modifier.matchParentSize().graphicsLayer { rotationZ = angle }.clip(MaterialShapes.Sunny.toShape()).background(scheme.tertiary))
                            Text(shown.level.toString(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = scheme.onTertiary)
                        }
                        Text("Level up!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("You've reached level ${shown.level}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

/** Total achievement XP, kept current as achievements unlock. */
@Composable
fun rememberAchievementXp(): Int {
    val context = LocalContext.current
    var xp by remember { mutableIntStateOf(Achievements.xp(context)) }
    DisposableEffect(Unit) {
        val prefs = context.getSharedPreferences("achievements", Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> xp = Achievements.xp(context) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return xp
}

/** Tracks the collection and surfaces newly unlocked achievements and level-ups. */
@Composable
fun rememberAchievementQueue(collection: TimetableCollection?, celebrate: Boolean, level: Int?, checkInXp: Int? = null, scope: String? = null): Pair<List<Celebration>, () -> Unit> {
    val context = LocalContext.current
    var queue by remember { mutableStateOf(emptyList<Celebration>()) }
    var firstRun by remember { mutableStateOf(true) }
    var events by remember { mutableIntStateOf(0) }
    var lastLevel by remember(scope) { mutableStateOf<Int?>(null) }
    var lastCheckInXp by remember(scope) { mutableStateOf<Int?>(null) }
    // Check-ins: show the XP filling up rather than a floating number.
    LaunchedEffect(checkInXp, scope) {
        val previous = lastCheckInXp
        if (checkInXp != null && previous != null && checkInXp > previous && celebrate) {
            val bonus = Achievements.xp(context)
            queue = queue + Celebration.XpGain(previous + bonus, checkInXp + bonus)
        }
        lastCheckInXp = checkInXp
    }
    LaunchedEffect(level, scope) {
        val previous = lastLevel
        if (level != null && previous != null && level > previous && celebrate) queue = queue + Celebration.LevelUp(level)
        lastLevel = level
    }
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
        val base = checkInXp?.let { it + Achievements.xp(context) - fresh.size * Achievements.XP }
        if (celebrate && !alreadyHad) queue = queue + fresh.mapIndexed { i, a -> Celebration.Unlocked(a, base?.let { it + (i + 1) * Achievements.XP }) }
    }
    return queue to { queue = queue.drop(1) }
}

/**
 * A short fall of Material Expressive shapes (about 2–3 s), released over the first half and falling at different speeds, clipped to its container and drawn
 * behind content. Skipped when system animations are off.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FallingConfetti(colours: List<androidx.compose.ui.graphics.Color>, modifier: Modifier = Modifier, durationMillis: Int = 2_400, count: Int = 44,
    corner: androidx.compose.ui.unit.Dp? = null) {
    val context = LocalContext.current
    val animationsOn = remember { android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
    if (!animationsOn) return
    val shapes = listOf(MaterialShapes.Cookie4Sided, MaterialShapes.Clover4Leaf, MaterialShapes.Sunny, MaterialShapes.Pill, MaterialShapes.Circle, MaterialShapes.Gem).map { it.toShape() }
    val pieces = remember {
        val random = kotlin.random.Random(System.nanoTime())
        List(count) { index ->
            // A real mix of sizes: mostly small pieces, some medium, and a few big shapes that spin and sink slowly.
            val big = index % 11 == 0
            val medium = !big && index % 4 == 0
            val size = when { big -> 26f + random.nextFloat() * 18f; medium -> 11f + random.nextFloat() * 6f; else -> 4f + random.nextFloat() * 5f }
            val speed = if (big) 0.4f + random.nextFloat() * 0.2f else 0.6f + random.nextFloat() * 0.9f
            val spin = (random.nextFloat() - .5f) * (if (big) 200f else 620f)
            listOf(random.nextFloat(), 0f, size, spin, random.nextFloat() * .55f, random.nextInt(shapes.size).toFloat(),
                random.nextInt(colours.size).toFloat(), if (big) 1f else 0f, speed)
        }
    }
    // Pieces are released over the first part, each falls at its own speed, and the animation lasts
    // exactly until the slowest piece has left the bottom, so nothing vanishes mid-air.
    val releaseMillis = durationMillis * .5f
    val baseFallMillis = durationMillis * .5f
    val totalMillis = remember(pieces) { pieces.maxOf { it[4] * releaseMillis / .55f + baseFallMillis / it[8] } }
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, androidx.compose.animation.core.tween(totalMillis.toInt(), easing = androidx.compose.animation.core.LinearEasing)) }
    val layout = androidx.compose.ui.platform.LocalLayoutDirection.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.foundation.Canvas(modifier) {
        val elapsed = progress.value * totalMillis
        if (progress.value <= 0f || progress.value >= 1f) return@Canvas
        // Clip to the pill in the drawing itself, so pieces never spill outside the notice.
        val pill = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, androidx.compose.ui.geometry.CornerRadius(corner?.toPx() ?: (size.height / 2))))
        }
        clipPath(pill) {
            pieces.forEach { piece ->
                val start = piece[4] * releaseMillis / .55f
                val local = ((elapsed - start) / (baseFallMillis / piece[8])).coerceIn(0f, 1f)
                if (local <= 0f || local >= 1f) return@forEach
                val side = piece[2].dp.toPx()
                // Straight down, easing in like gravity, from above the top edge to below the bottom.
                val px = size.width * piece[0]
                val py = -side + (size.height + side * 2) * (local * (0.35f + 0.65f * local))
                val outline = shapes[piece[5].toInt()].createOutline(androidx.compose.ui.geometry.Size(side, side), layout, density)
                drawContext.canvas.save()
                drawContext.transform.translate(px - side / 2, py)
                drawContext.transform.rotate(piece[3] * local, androidx.compose.ui.geometry.Offset(side / 2, side / 2))
                // Big shapes are softer so the text on top stays easy to read.
                val colour = colours[piece[6].toInt()]
                drawOutline(outline, colour, alpha = if (piece[7] == 1f) .35f else .85f)
                drawContext.canvas.restore()
            }
        }
    }
}

