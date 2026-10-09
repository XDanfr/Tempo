package cc.xdan.tempo.model

import kotlinx.serialization.Serializable

@Serializable
data class TimeSpan(val start: Int, val end: Int) {
    init { require(start in 0..1439 && end in 1..1440 && end > start) { "Times must form a positive interval within one day" } }
    val minutes: Int get() = end - start
}

@Serializable
enum class SubjectIcon {
    NONE, CODE, SCIENCE, MUSIC, PERSON, WORK, BOOK, MATHS, ART, HISTORY, LANGUAGES, GEOGRAPHY, BIOLOGY, PHYSICS, LITERATURE, WRITING, ECONOMICS, BUSINESS, LAW, MEDICINE, PSYCHOLOGY, SPORT, FITNESS, DANCE, DRAMA, FILM, PHOTOGRAPHY, DESIGN, ENGINEERING, ROBOTICS, ELECTRONICS, GAMING, NETWORKING, SECURITY, TRAVEL, CAR, BUS, BIKE, WALK, COFFEE, LUNCH, BREAK, CHANGEOVER, REST, SHOPPING, VOLUNTEER, MEETING, CALL, EMAIL, HOME, OFFICE, LIBRARY, CALENDAR, TIME, STAR, HEART, IDEA, CHECK, FOLDER, TOOLS, NATURE, PETS, ACCESSIBILITY
}

@Serializable
data class Subject(
    val id: String,
    val name: String,
    val colour: Long = 0xFFB7F397,
    val icon: SubjectIcon = SubjectIcon.NONE,
    val location: String = "",
) { init { require(id.isNotBlank() && name.isNotBlank()) } }

@Serializable
data class Session(
    val id: String,
    val subjectId: String,
    val day: Int,
    val time: TimeSpan,
    val location: String = "",
    val notes: String = "",
) { init { require(id.isNotBlank() && subjectId.isNotBlank() && day in 1..7) } }

@Serializable
data class DayHours(val day: Int, val time: TimeSpan) { init { require(day in 1..7) } }

@Serializable
data class PeriodTemplate(val id: String, val name: String, val time: TimeSpan) {
    init { require(id.isNotBlank() && name.isNotBlank()) }
}

@Serializable
enum class ThemePreset { FOREST, OCEAN, AMBER, MATERIAL_YOU }
@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class Appearance(
    val preset: ThemePreset = ThemePreset.FOREST,
    val mode: ThemeMode = ThemeMode.DARK,
    val dynamicColour: Boolean = false,
    val showIcons: Boolean = true,
    val blockStyle: BlockStyle = BlockStyle.FILLED,
    val breakStyle: BlockStyle? = null,
) {
    // Keep the first milestone's wallpaper switch readable when migrating saved state.
    val effectiveBreakStyle: BlockStyle get() = breakStyle ?: blockStyle
    val selectedPreset: ThemePreset get() = if (dynamicColour) ThemePreset.MATERIAL_YOU else preset
}

@Serializable
enum class BlockStyle { FILLED, OUTLINED }

@Serializable
enum class BreakKind { BREAK, LUNCH, CHANGEOVER, CUSTOM }

@Serializable
data class ScheduledBreak(
    val id: String,
    val name: String,
    val days: List<Int>,
    val time: TimeSpan,
    val kind: BreakKind = BreakKind.BREAK,
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        require(days.isNotEmpty() && days.all { it in 1..7 } && days.distinct().size == days.size)
    }
}

@Serializable
data class Timetable(
    val schemaVersion: Int = 2,
    val name: String = "My timetable",
    val onboarded: Boolean = false,
    val onboardingStep: Int = 0,
    val subjects: List<Subject> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val breaks: List<ScheduledBreak> = emptyList(),
    val days: List<DayHours> = (1..5).map { DayHours(it, TimeSpan(540, 990)) },
    val periods: List<PeriodTemplate> = listOf(
        PeriodTemplate("p1", "Period 1", TimeSpan(540, 630)),
        PeriodTemplate("p2", "Period 2", TimeSpan(645, 735)),
        PeriodTemplate("p3", "Period 3", TimeSpan(780, 870)),
        PeriodTemplate("p4", "Period 4", TimeSpan(885, 975)),
    ),
    val appearance: Appearance = Appearance(),
) {
    init {
        require(onboardingStep in 0..3)
        require(schemaVersion == 2) { "Unsupported timetable version" }
        require(name.isNotBlank())
        require(subjects.map { it.id }.distinct().size == subjects.size)
        require(sessions.map { it.id }.distinct().size == sessions.size)
        require(breaks.map { it.id }.distinct().size == breaks.size)
        require(days.map { it.day }.distinct().size == days.size)
        require(periods.map { it.id }.distinct().size == periods.size)
        require(sessions.all { s -> subjects.any { it.id == s.subjectId } }) { "Unknown session subject" }
    }
}

fun minuteLabel(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)
fun parseMinute(text: String): Int? {
    if (!Regex("[0-9]{1,2}:[0-9]{2}").matches(text)) return null
    val parts = text.split(":").map { it.toInt() }
    return if (parts[0] in 0..23 && parts[1] in 0..59) parts[0] * 60 + parts[1]
    else if (parts[0] == 24 && parts[1] == 0) 1440 else null
}

@Serializable
data class SavedTimetable(val id: String, val timetable: Timetable) {
    init { require(id.isNotBlank()) }
}

@Serializable
data class TimetableCollection(
    val format: String = "cc.xdan.tempo.library",
    val formatVersion: Int = 1,
    val activeId: String = "local",
    val timetables: List<SavedTimetable> = listOf(SavedTimetable("local", Timetable())),
) {
    init {
        require(format == "cc.xdan.tempo.library" && formatVersion == 1) { "Unsupported timetable collection" }
        require(timetables.isNotEmpty()) { "Keep at least one timetable" }
        require(timetables.map { it.id }.distinct().size == timetables.size) { "Duplicate timetable IDs" }
        require(timetables.any { it.id == activeId }) { "Active timetable not found" }
    }
    val active: Timetable get() = timetables.first { it.id == activeId }.timetable
    fun update(id: String, transform: (Timetable) -> Timetable): TimetableCollection {
        require(timetables.any { it.id == id }) { "Timetable no longer exists" }
        return copy(timetables = timetables.map { if (it.id == id) it.copy(timetable = transform(it.timetable)) else it })
    }
    fun add(id: String, timetable: Timetable): TimetableCollection =
        copy(activeId = id, timetables = timetables + SavedTimetable(id, timetable))
    fun select(id: String): TimetableCollection = copy(activeId = id)
    fun remove(id: String): TimetableCollection {
        require(timetables.any { it.id == id }) { "Timetable no longer exists" }
        require(timetables.size > 1) { "Keep at least one timetable" }
        val remaining = timetables.filterNot { it.id == id }
        return copy(activeId = if (activeId == id) remaining.first().id else activeId, timetables = remaining)
    }
}
