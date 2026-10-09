package cc.xdan.tempo.data

import cc.xdan.tempo.model.Timetable
import kotlinx.serialization.json.*
import java.io.InputStream
import java.io.ByteArrayOutputStream

/** Portable snapshots include all subjects, sessions, breaks, hours, periods and appearance. */
object TimetableTransfer {
    const val MAX_BYTES = 2 * 1024 * 1024
    private val json = Json { prettyPrint = true }
    fun encode(timetable: Timetable): String = json.encodeToString(JsonObject.serializer(), buildJsonObject {
        put("format", "cc.xdan.tempo.timetable")
        put("formatVersion", 1)
        put("timetable", json.parseToJsonElement(TimetableCodec.encode(timetable)))
    })

    fun read(input: InputStream): Timetable {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            require(output.size() + count <= MAX_BYTES) { "Timetable files must be smaller than 2 MB" }
            output.write(buffer, 0, count)
        }
        return decode(output.toString(Charsets.UTF_8.name()))
    }

    fun decode(text: String): Timetable {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Timetable files must be smaller than 2 MB" }
        checkNesting(text)
        val root = json.parseToJsonElement(text).jsonObject
        val payload = if (root["format"] == null) root else {
            require(root.keys == setOf("format", "formatVersion", "timetable")) { "Unexpected timetable file fields" }
            require(root["format"]?.jsonPrimitive?.content == "cc.xdan.tempo.timetable") { "This is not a Tempo timetable file" }
            require(root["formatVersion"]?.jsonPrimitive?.int == 1) { "This timetable file needs a newer version of Tempo" }
            root.getValue("timetable").jsonObject
        }
        val result = TimetableCodec.decode(payload.toString())
        require(result.subjects.all { it.colour in 0..0xFFFFFFFFL }) { "Subject colours must be valid ARGB values" }
        return result
    }

    private fun checkNesting(text: String) {
        var depth = 0
        var inString = false
        var escaped = false
        for (char in text) {
            if (inString) {
                if (escaped) escaped = false
                else if (char == '\\') escaped = true
                else if (char == '"') inString = false
            } else when (char) {
                '"' -> inString = true
                '{', '[' -> { depth++; require(depth <= 32) { "Timetable file is nested too deeply" } }
                '}', ']' -> depth--
            }
        }
    }
}
