package cc.xdan.tempo.share

import android.content.Context
import cc.xdan.tempo.data.ShareLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** The owner's side of a link: kept only in this app's private storage. */
data class OwnedShare(val id: String, val key: String, val token: String, val rooms: Boolean, val notes: Boolean) {
    val link get() = ShareLink(id, key)
}

/** Owned links per timetable, in app-private preferences. Owner tokens never leave the device. */
object ShareStore {
    private fun prefs(context: Context) = context.getSharedPreferences("shares", Context.MODE_PRIVATE)
    fun owned(context: Context, timetableId: String): OwnedShare? = prefs(context).getString("own:$timetableId", null)?.split("|")?.takeIf { it.size == 5 }
        ?.let { OwnedShare(it[0], it[1], it[2], it[3] == "1", it[4] == "1") }
    fun save(context: Context, timetableId: String, share: OwnedShare) = prefs(context).edit()
        .putString("own:$timetableId", listOf(share.id, share.key, share.token, if (share.rooms) "1" else "0", if (share.notes) "1" else "0").joinToString("|")).apply()
    fun forget(context: Context, timetableId: String) = prefs(context).edit().remove("own:$timetableId").remove("hash:$timetableId").apply()
    /** Hash of the last uploaded content, so unchanged timetables aren't re-uploaded. */
    fun uploadedHash(context: Context, timetableId: String) = prefs(context).getString("hash:$timetableId", null)
    fun setUploadedHash(context: Context, timetableId: String, hash: String) = prefs(context).edit().putString("hash:$timetableId", hash).apply()
    /** Someone else's link this timetable came from, and the revision we have. */
    data class Followed(val link: ShareLink, val rev: Long)
    fun follow(context: Context, timetableId: String, link: ShareLink, rev: Long = 0) =
        prefs(context).edit().putString("follow:$timetableId", "${link.id}|${link.key}|$rev").apply()
    fun followed(context: Context): Map<String, Followed> = prefs(context).all.mapNotNull { (key, value) ->
        if (!key.startsWith("follow:")) return@mapNotNull null
        val parts = (value as? String)?.split("|")?.takeIf { it.size == 3 } ?: return@mapNotNull null
        key.removePrefix("follow:") to Followed(ShareLink(parts[0], parts[1]), parts[2].toLongOrNull() ?: 0)
    }.toMap()
    fun unfollow(context: Context, timetableId: String) = prefs(context).edit().remove("follow:$timetableId").apply()
    fun allOwned(context: Context): Map<String, OwnedShare> = prefs(context).all.keys.filter { it.startsWith("own:") }
        .mapNotNull { key -> owned(context, key.removePrefix("own:"))?.let { key.removePrefix("own:") to it } }.toMap()
}

/** Friendly failure with the server's reason; [gone] means the owner stopped sharing. */
class ShareException(message: String, val gone: Boolean = false) : Exception(message)

/** Talks to the share Worker. Only ciphertext and the owner token are ever sent. */
object ShareClient {
    private const val API = "https://${ShareLink.HOST}/api/t"

    private suspend fun call(method: String, path: String = "", body: ByteArray? = null, token: String? = null): Pair<Int, ByteArray> = withContext(Dispatchers.IO) {
        val connection = URL(API + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000; connection.readTimeout = 20_000
            connection.setRequestProperty("User-Agent", "Tempo-Android")
            token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) { connection.doOutput = true; connection.setRequestProperty("Content-Type", "application/octet-stream"); connection.outputStream.use { it.write(body) } }
            val code = connection.responseCode
            val stream = if (code >= 400) connection.errorStream else connection.inputStream
            code to (stream?.use { input -> input.readBytes().also { require(it.size <= 128 * 1024) } } ?: ByteArray(0))
        } catch (e: java.io.IOException) {
            throw ShareException("Couldn't reach Tempo's sharing service. Check your connection and try again.")
        } finally { connection.disconnect() }
    }

    private fun failure(code: Int, body: ByteArray): Nothing {
        val reason = runCatching { JSONObject(body.toString(Charsets.UTF_8)).getString("error") }.getOrNull()
        throw ShareException(reason ?: "Sharing didn't work (error $code). Try again later.", gone = code == 410 || code == 404)
    }

    suspend fun create(ciphertext: ByteArray): Pair<String, String> {
        val (code, body) = call("POST", body = ciphertext)
        if (code != 201) failure(code, body)
        val json = JSONObject(body.toString(Charsets.UTF_8))
        return json.getString("id") to json.getString("ownerToken")
    }
    suspend fun fetch(id: String): ByteArray { val (code, body) = call("GET", "/$id"); if (code != 200) failure(code, body); return body }

    /** Downloads only when the revision changed: returns null when unchanged, otherwise the new revision and ciphertext. */
    suspend fun fetchIfChanged(id: String, rev: Long): Pair<Long, ByteArray>? = withContext(Dispatchers.IO) {
        val connection = URL("$API/$id").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000; connection.readTimeout = 20_000
            connection.setRequestProperty("User-Agent", "Tempo-Android")
            if (rev > 0) connection.setRequestProperty("If-None-Match", "\"$rev\"")
            when (val code = connection.responseCode) {
                304 -> null
                200 -> (connection.getHeaderField("x-tempo-rev")?.toLongOrNull() ?: 0L) to connection.inputStream.use { it.readBytes() }
                else -> failure(code, connection.errorStream?.use { it.readBytes() } ?: ByteArray(0))
            }
        } catch (e: java.io.IOException) { throw ShareException("Couldn't reach Tempo's sharing service.") }
        finally { connection.disconnect() }
    }
    suspend fun update(id: String, token: String, ciphertext: ByteArray) { val (code, body) = call("PUT", "/$id", ciphertext, token); if (code != 200) failure(code, body) }
    suspend fun delete(id: String, token: String) { val (code, body) = call("DELETE", "/$id", token = token); if (code != 204 && code != 410) failure(code, body) }
}
