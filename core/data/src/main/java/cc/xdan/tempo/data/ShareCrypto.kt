package cc.xdan.tempo.data

import cc.xdan.tempo.model.Timetable
import kotlinx.serialization.json.*
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** What a person chose to include when sharing. Rooms and notes are off by default. */
data class ShareOptions(val rooms: Boolean = false, val notes: Boolean = false)

/** A decrypted share: whose it is and the timetable they shared. */
data class SharedTimetable(val sharer: String, val timetable: Timetable)

/** A parsed share link: `https://tempo.xdan.cc/t/<id>#k=<key>` or `tempo://t/<id>#k=<key>`. */
data class ShareLink(val id: String, val key: String) {
    val url get() = "https://$HOST/t/$id#k=$key"
    companion object {
        const val HOST = "tempo.xdan.cc"
        private val ID = Regex("^[A-Za-z0-9_-]{22}$")
        private val KEY = Regex("^[A-Za-z0-9_-]{43}$")
        fun parse(text: String): ShareLink? {
            val trimmed = text.trim()
            val match = Regex("^(?:https://${Regex.escape(HOST)}/t/|tempo://t/)([^#/?]+)#(?:.*&)?k=([^&]+).*$").find(trimmed) ?: return null
            val (id, key) = match.destructured
            return if (ID.matches(id) && KEY.matches(key)) ShareLink(id, key) else null
        }
    }
}

/**
 * End-to-end encryption for shared timetables. A fresh 256-bit key is made per link and only ever
 * travels in the link's #fragment. Ciphertext is a 12-byte IV followed by AES-GCM output, so any
 * tampering or wrong key fails authentication instead of producing garbage.
 */
object ShareCrypto {
    private const val FORMAT = "cc.xdan.tempo.share"
    private val random = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()
    private val json = Json

    fun newKey(): String = ByteArray(32).also(random::nextBytes).let(encoder::encodeToString)

    /** Removes what shouldn't leave the device: rooms and notes unless chosen, and personal progress always. */
    fun redact(timetable: Timetable, options: ShareOptions): Timetable = timetable.copy(
        subjects = timetable.subjects.map { if (options.rooms) it else it.copy(location = "") },
        sessions = timetable.sessions.map { it.copy(location = if (options.rooms) it.location else "", notes = if (options.notes) it.notes else "") },
        exceptions = timetable.exceptions.map { day -> day.copy(sessions = day.sessions.map { it.copy(location = if (options.rooms) it.location else "", notes = if (options.notes) it.notes else "") }) },
        periods = timetable.periods.map { if (options.rooms) it else it.copy(location = "") },
        checkIns = emptyList(), person = null, setupStep = null, progress = cc.xdan.tempo.model.ProgressMode.CELEBRATIONS,
    )

    fun encrypt(sharer: String, timetable: Timetable, options: ShareOptions, key: String): ByteArray {
        val plain = json.encodeToString(JsonObject.serializer(), buildJsonObject {
            put("format", FORMAT); put("formatVersion", 1); put("sharer", sharer.take(40))
            put("timetable", json.parseToJsonElement(TimetableTransfer.encode(redact(timetable, options))))
        }).toByteArray(Charsets.UTF_8)
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(decoder.decode(key), "AES"), GCMParameterSpec(128, iv))
        return iv + cipher.doFinal(plain)
    }

    fun decrypt(data: ByteArray, key: String): SharedTimetable {
        require(data.size > 28) { "This shared timetable is damaged" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(decoder.decode(key), "AES"), GCMParameterSpec(128, data.copyOfRange(0, 12)))
        val plain = try { cipher.doFinal(data, 12, data.size - 12) } catch (e: Exception) { throw IllegalArgumentException("This link's key doesn't match its timetable") }
        val root = json.parseToJsonElement(plain.toString(Charsets.UTF_8)).jsonObject
        require(root["format"]?.jsonPrimitive?.content == FORMAT) { "This isn't a Tempo shared timetable" }
        require(root["formatVersion"]?.jsonPrimitive?.int == 1) { "This shared timetable needs a newer version of Tempo" }
        val sharer = root["sharer"]?.jsonPrimitive?.content.orEmpty().take(40)
        return SharedTimetable(sharer, TimetableTransfer.decode(root.getValue("timetable").toString()))
    }
}
