package cc.xdan.tempo.account

import android.app.Activity
import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import cc.xdan.tempo.data.ShareCrypto
import cc.xdan.tempo.data.ShareLink
import cc.xdan.tempo.share.ShareClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** What's kept on the device for an account. The profile key never leaves your devices. */
data class SignedIn(val accountId: String, val session: String, val profileKey: String)

/** The profile friends will see, stored on the server only as ciphertext. */
data class Profile(val name: String, val level: Int, val achievements: Int) {
    fun toJson(): ByteArray = JSONObject().put("name", name.take(40)).put("level", level).put("achievements", achievements).toString().toByteArray()
    companion object {
        fun fromJson(bytes: ByteArray) = JSONObject(String(bytes)).let { Profile(it.optString("name"), it.optInt("level", 1), it.optInt("achievements")) }
    }
}

object AccountStore {
    private fun prefs(context: Context) = context.getSharedPreferences("account", Context.MODE_PRIVATE)
    fun current(context: Context): SignedIn? = prefs(context).let { p ->
        val id = p.getString("accountId", null) ?: return null
        val session = p.getString("session", null) ?: return null
        SignedIn(id, session, p.getString("profileKey", null) ?: return null)
    }
    fun save(context: Context, account: SignedIn) {
        prefs(context).edit().putString("accountId", account.accountId).putString("session", account.session).putString("profileKey", account.profileKey).apply()
        ShareClient.session = account.session
    }
    fun clear(context: Context) { prefs(context).edit().clear().apply(); ShareClient.session = null }
    /** Call once at start so shares made while signed in are account-owned. */
    fun restore(context: Context) { ShareClient.session = current(context)?.session }
}

/** Friendly failure; [cancelled] when the person closed the passkey prompt. */
class AccountException(message: String, val cancelled: Boolean = false) : Exception(message)

/** Passkey sign-up and sign-in against tempo.xdan.cc, plus the account's encrypted profile. */
object Accounts {
    private const val API = "https://${ShareLink.HOST}/api"

    private suspend fun call(method: String, path: String, json: JSONObject? = null, bytes: ByteArray? = null, session: String? = null): Pair<Int, String> = withContext(Dispatchers.IO) {
        val connection = URL(API + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000; connection.readTimeout = 20_000
            connection.setRequestProperty("User-Agent", "Tempo-Android")
            session?.let { connection.setRequestProperty("X-Tempo-Session", it) }
            val body = json?.toString()?.toByteArray() ?: bytes
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", if (json != null) "application/json" else "application/octet-stream")
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            code to ((if (code >= 400) connection.errorStream else connection.inputStream)?.use { String(it.readBytes()) } ?: "")
        } catch (e: java.io.IOException) {
            throw AccountException("Couldn't reach Tempo. Check your connection and try again.")
        } finally { connection.disconnect() }
    }

    private fun fail(code: Int, body: String): Nothing =
        throw AccountException(runCatching { JSONObject(body).getString("error") }.getOrNull() ?: "Something went wrong (error $code). Try again.")

    private suspend fun challenge(kind: String): JSONObject {
        val (code, body) = call("POST", "/auth/challenge", JSONObject().put("kind", kind))
        if (code != 200) fail(code, body)
        return JSONObject(body)
    }

    /** Creates a passkey and an account. [name] labels the passkey in the password manager. */
    suspend fun create(activity: Activity, name: String): SignedIn {
        val c = challenge("register")
        val request = JSONObject()
            .put("challenge", c.getString("challenge"))
            .put("rp", JSONObject().put("id", c.getString("rpId")).put("name", "Tempo"))
            .put("user", JSONObject().put("id", c.getString("userId")).put("name", name.ifBlank { "Tempo" }).put("displayName", name.ifBlank { "Tempo" }))
            .put("pubKeyCredParams", JSONArray().put(JSONObject().put("type", "public-key").put("alg", -7)).put(JSONObject().put("type", "public-key").put("alg", -257)))
            .put("timeout", 120_000)
            .put("attestation", "none")
            .put("authenticatorSelection", JSONObject().put("residentKey", "required").put("requireResidentKey", true).put("userVerification", "preferred"))
        val result = try {
            CredentialManager.create(activity).createCredential(activity, CreatePublicKeyCredentialRequest(request.toString())) as CreatePublicKeyCredentialResponse
        } catch (e: CreateCredentialCancellationException) { throw AccountException("Cancelled", cancelled = true) }
        catch (e: Exception) { throw AccountException("Couldn't create a passkey: ${e.message ?: "try again"}") }
        val credential = JSONObject(result.registrationResponseJson)
        val (code, body) = call("POST", "/auth/register", JSONObject().put("challengeId", c.getString("challengeId")).put("response", credential.getJSONObject("response").put("id", credential.getString("id"))))
        if (code != 201) fail(code, body)
        val json = JSONObject(body)
        return SignedIn(json.getString("accountId"), json.getString("session"), ShareCrypto.newKey())
    }

    /**
     * Signs in with an existing passkey. The profile key only exists on the device that made the account
     * until device sync arrives, so a new device starts a fresh profile key.
     */
    suspend fun signIn(activity: Activity, profileKey: String? = null): SignedIn {
        val c = challenge("login")
        val request = JSONObject().put("challenge", c.getString("challenge")).put("rpId", c.getString("rpId"))
            .put("timeout", 120_000).put("userVerification", "preferred").put("allowCredentials", JSONArray())
        val result = try {
            CredentialManager.create(activity).getCredential(activity, GetCredentialRequest(listOf(GetPublicKeyCredentialOption(request.toString()))))
        } catch (e: GetCredentialCancellationException) { throw AccountException("Cancelled", cancelled = true) }
        catch (e: NoCredentialException) { throw AccountException("No Tempo passkey on this device. Create an account first.") }
        catch (e: Exception) { throw AccountException("Couldn't sign in: ${e.message ?: "try again"}") }
        val credential = JSONObject((result.credential as? PublicKeyCredential)?.authenticationResponseJson ?: throw AccountException("That wasn't a passkey"))
        val (code, body) = call("POST", "/auth/login", JSONObject().put("challengeId", c.getString("challengeId")).put("credentialId", credential.getString("id"))
            .put("response", credential.getJSONObject("response").put("id", credential.getString("id"))))
        if (code != 200) fail(code, body)
        val json = JSONObject(body)
        return SignedIn(json.getString("accountId"), json.getString("session"), profileKey ?: ShareCrypto.newKey())
    }

    suspend fun saveProfile(account: SignedIn, profile: Profile) {
        val (code, body) = call("PUT", "/me/profile", bytes = ShareCrypto.seal(profile.toJson(), account.profileKey), session = account.session)
        if (code != 204) fail(code, body)
    }

    /** IDs of links this account owns. */
    suspend fun ownedShares(account: SignedIn): Set<String> {
        val (code, body) = call("GET", "/me", session = account.session)
        if (code != 200) fail(code, body)
        val ids = JSONObject(body).getJSONArray("shares")
        return (0 until ids.length()).map { ids.getString(it) }.toSet()
    }

    /** Ends this device's session. */
    suspend fun signOut(account: SignedIn) { runCatching { call("DELETE", "/me/session", session = account.session) } }

    /** Deletes the account, its passkey record, sessions, profile and every link it owns. */
    suspend fun delete(account: SignedIn) {
        val (code, body) = call("DELETE", "/me", session = account.session)
        if (code != 204 && code != 401) fail(code, body)
    }
}
