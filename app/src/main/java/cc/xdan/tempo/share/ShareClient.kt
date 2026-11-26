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
    fun forget(context: Context, timetableId: String) = prefs(context).edit().remove("own:$timetableId").apply()
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
    suspend fun update(id: String, token: String, ciphertext: ByteArray) { val (code, body) = call("PUT", "/$id", ciphertext, token); if (code != 200) failure(code, body) }
    suspend fun delete(id: String, token: String) { val (code, body) = call("DELETE", "/$id", token = token); if (code != 204 && code != 410) failure(code, body) }
}
