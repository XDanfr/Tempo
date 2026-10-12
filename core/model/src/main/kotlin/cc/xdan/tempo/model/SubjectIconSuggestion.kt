package cc.xdan.tempo.model

import java.util.Locale

private val subjectIconAliases = mapOf(
    "maths" to SubjectIcon.MATHS, "mathematics" to SubjectIcon.MATHS, "math" to SubjectIcon.MATHS,
    "biology" to SubjectIcon.BIOLOGY, "chemistry" to SubjectIcon.SCIENCE, "science" to SubjectIcon.SCIENCE,
    "physics" to SubjectIcon.PHYSICS, "computer science" to SubjectIcon.CODE, "computing" to SubjectIcon.CODE,
    "programming" to SubjectIcon.CODE, "history" to SubjectIcon.HISTORY, "geography" to SubjectIcon.GEOGRAPHY,
    "english" to SubjectIcon.LITERATURE, "english literature" to SubjectIcon.LITERATURE,
    "french" to SubjectIcon.LANGUAGES, "spanish" to SubjectIcon.LANGUAGES, "german" to SubjectIcon.LANGUAGES,
    "languages" to SubjectIcon.LANGUAGES, "art" to SubjectIcon.ART, "design" to SubjectIcon.DESIGN,
    "music" to SubjectIcon.MUSIC, "drama" to SubjectIcon.DRAMA, "dance" to SubjectIcon.DANCE,
    "pe" to SubjectIcon.SPORT, "physical education" to SubjectIcon.SPORT, "sport" to SubjectIcon.SPORT,
    "economics" to SubjectIcon.ECONOMICS, "business" to SubjectIcon.BUSINESS,
    "psychology" to SubjectIcon.PSYCHOLOGY, "engineering" to SubjectIcon.ENGINEERING,
    "law" to SubjectIcon.LAW, "photography" to SubjectIcon.PHOTOGRAPHY,
)

fun suggestedSubjectIcon(name: String): SubjectIcon =
    subjectIconAliases[name.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")] ?: SubjectIcon.NONE
