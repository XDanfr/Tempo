package cc.xdan.tempo.data

import cc.xdan.tempo.model.Timetable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

/** Strict decoding keeps unknown versions or malformed references from becoming valid state. */
object TimetableCodec {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }
    fun encode(value: Timetable): String = json.encodeToString(value)
    fun decode(text: String): Timetable {
        val root = json.parseToJsonElement(text).jsonObject
        val version = root["schemaVersion"]?.jsonPrimitive?.int ?: 1
        require(version in 1..2) { "Unsupported timetable version: $version" }
        val migrated = if (version == 1) JsonObject(root.toMutableMap().apply {
            this["schemaVersion"] = JsonPrimitive(2)
            val step = root["onboardingStep"]?.jsonPrimitive?.int ?: 0
            val onboarded = root["onboarded"]?.jsonPrimitive?.boolean ?: false
            this["onboardingStep"] = JsonPrimitive(if (onboarded) step else when (step) { 1 -> 2; 2 -> 3; else -> step })
        }) else root
        return json.decodeFromJsonElement<Timetable>(migrated)
    }
}
