package cc.xdan.tempo.data

import cc.xdan.tempo.model.Timetable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Strict decoding keeps unknown versions or malformed references from becoming valid state. */
object TimetableCodec {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }
    fun encode(value: Timetable): String = json.encodeToString(value)
    fun decode(text: String): Timetable = json.decodeFromString<Timetable>(text)
}
