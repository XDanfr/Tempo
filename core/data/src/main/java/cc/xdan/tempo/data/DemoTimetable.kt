package cc.xdan.tempo.data

import cc.xdan.tempo.model.*

/** An illustrative college week, explicitly labelled in onboarding. */
fun demoTimetable(appearance: Appearance = Appearance()): Timetable {
    val subjects = listOf(
        Subject("cs", "Computer Science", 0xFFB7F397, SubjectIcon.CODE, "C12"),
        Subject("physics", "Physics", 0xFFD3BCFF, SubjectIcon.SCIENCE, "P04"),
        Subject("music", "Music Production", 0xFFA2DFFF, SubjectIcon.MUSIC, "Studio 2"),
        Subject("tutor", "Tutor", 0xFFFFD59B, SubjectIcon.PERSON, "C08"),
    )
    return Timetable(name = "Demo college week", onboarded = true, subjects = subjects, appearance = appearance,
        sessions = listOf(
            Session("m1", "cs", 1, TimeSpan(540, 630)), Session("m2", "music", 1, TimeSpan(780, 870)),
            Session("t1", "physics", 2, TimeSpan(645, 735)), Session("t2", "cs", 2, TimeSpan(885, 975)),
            Session("w1", "tutor", 3, TimeSpan(540, 600)), Session("w2", "music", 3, TimeSpan(645, 735)),
            Session("h1", "physics", 4, TimeSpan(540, 630)), Session("h2", "cs", 4, TimeSpan(780, 870)),
            Session("f1", "music", 5, TimeSpan(540, 630)), Session("f2", "physics", 5, TimeSpan(645, 735)),
        ))
}
