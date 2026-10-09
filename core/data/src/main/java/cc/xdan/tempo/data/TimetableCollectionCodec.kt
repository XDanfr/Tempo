package cc.xdan.tempo.data

import cc.xdan.tempo.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

object TimetableCollectionCodec {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }
    fun encode(collection: TimetableCollection): String = json.encodeToString(collection)
    fun decode(text: String): TimetableCollection {
        val root = json.parseToJsonElement(text).jsonObject
        return if (root["format"] == null) {
            TimetableCollection(timetables = listOf(SavedTimetable("local", TimetableCodec.decode(text))))
        } else {
            require(root["format"]?.jsonPrimitive?.content == "cc.xdan.tempo.library") { "Unsupported saved format" }
            json.decodeFromJsonElement<TimetableCollection>(root)
        }
    }
}
