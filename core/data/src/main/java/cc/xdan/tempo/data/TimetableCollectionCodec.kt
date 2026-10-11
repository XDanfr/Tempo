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
            val migrated = JsonObject(root.toMutableMap().apply {
                this["timetables"] = JsonArray(root.getValue("timetables").jsonArray.map { entry ->
                    JsonObject(entry.jsonObject.toMutableMap().apply {
                        this["timetable"] = json.parseToJsonElement(TimetableCodec.encode(TimetableCodec.decode(getValue("timetable").toString())))
                    })
                })
            })
            json.decodeFromJsonElement<TimetableCollection>(migrated)
        }
    }
}
