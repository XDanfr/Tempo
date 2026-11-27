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
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * What's kept on the device for an account. [vaultKey] encrypts the cloud copy and the profile; it comes from
 * the passkey (PRF) or a recovery code, so the server never has it. Null until unlocked on this device.
 */
data class SignedIn(val accountId: String, val session: String, val vaultKey: String?)

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
        SignedIn(id, session, p.getString("vaultKey", null))
    }
    fun save(context: Context, account: SignedIn) {
        prefs(context).edit().putString("accountId", account.accountId).putString("session", account.session)
            .putString("vaultKey", account.vaultKey).remove("profileKey").apply()
        ShareClient.session = account.session
    }
    fun clear(context: Context) {
        prefs(context).edit().clear().apply(); ShareClient.session = null
        context.getSharedPreferences("cloud", Context.MODE_PRIVATE).edit().clear().apply()
    }
    /** Call once at start so shares made while signed in are account-owned. */
    fun restore(context: Context) { ShareClient.session = current(context)?.session }
}

/** Friendly failure; [cancelled] when the person closed the passkey prompt. */
class AccountException(message: String, val cancelled: Boolean = false) : Exception(message)

/**
 * Result of signing up or in. [recoveryCode] is a new code to show once (passkey provider without PRF);
 * [needsRecoveryCode] is set when this device must enter the account's recovery code to unlock the vault.
 */
data class AccountResult(val account: SignedIn, val recoveryCode: String? = null, val needsRecoveryCode: String? = null)

/** Passkey sign-up and sign-in against tempo.xdan.cc, the vault key, and the account's encrypted profile. */
object Accounts {
    private const val API = "https://${ShareLink.HOST}/api"
    private val b64 = Base64.getUrlEncoder().withoutPadding()
    private val unb64 = Base64.getUrlDecoder()
    /** Fixed PRF input: the passkey turns it into a secret only it can reproduce. */
    private val prfSalt = b64.encodeToString(MessageDigest.getInstance("SHA-256").digest("tempo-vault-v1".toByteArray()))
    private fun prfExtension() = JSONObject().put("prf", JSONObject().put("eval", JSONObject().put("first", prfSalt)))

    private suspend fun call(method: String, path: String, json: JSONObject? = null, bytes: ByteArray? = null, session: String? = null,
        headers: Map<String, String> = emptyMap()): Triple<Int, ByteArray, Map<String, List<String>>> = withContext(Dispatchers.IO) {
        val connection = URL(API + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000; connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", "Tempo-Android")
            session?.let { connection.setRequestProperty("X-Tempo-Session", it) }
            headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
            val body = json?.toString()?.toByteArray() ?: bytes
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", if (json != null) "application/json" else "application/octet-stream")
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            Triple(code, (if (code >= 400) connection.errorStream else connection.inputStream)?.use { it.readBytes() } ?: ByteArray(0), connection.headerFields.filterKeys { it != null })
        } catch (e: java.io.IOException) {
            throw AccountException("Couldn't reach Tempo. Check your connection and try again.")
        } finally { connection.disconnect() }
    }
    private suspend fun callJson(method: String, path: String, json: JSONObject? = null, session: String? = null): Pair<Int, String> =
        call(method, path, json, session = session).let { it.first to String(it.second) }

    private fun fail(code: Int, body: String): Nothing =
        throw AccountException(runCatching { JSONObject(body).getString("error") }.getOrNull() ?: "Something went wrong (error $code). Try again.")

    private suspend fun challenge(kind: String): JSONObject {
        val (code, body) = callJson("POST", "/auth/challenge", JSONObject().put("kind", kind))
        if (code != 200) fail(code, body)
        return JSONObject(body)
    }

    private fun prfResult(credential: JSONObject): ByteArray? = credential.optJSONObject("clientExtensionResults")?.optJSONObject("prf")
        ?.optJSONObject("results")?.optString("first")?.takeIf { it.isNotEmpty() }?.let { unb64.decode(it) }
    /** Key that wraps the vault key, from the passkey's PRF output. */
    private fun wrapKey(prf: ByteArray) = b64.encodeToString(MessageDigest.getInstance("SHA-256").digest(prf + "tempo-wrap".toByteArray()))
    /** Key that wraps the vault key, from a recovery code. Slow on purpose. */
    private fun recoveryKey(code: String, accountId: String): String {
        val clean = code.uppercase().filter { it.isLetterOrDigit() }
        val spec = PBEKeySpec(clean.toCharArray(), accountId.toByteArray(), 210_000, 256)
        return b64.encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded)
    }
    private fun newRecoveryCode(): String {
        val alphabet = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        val random = java.security.SecureRandom()
        return (1..20).map { alphabet[random.nextInt(alphabet.length)] }.chunked(5).joinToString("-") { it.joinToString("") }
    }
    private fun wrap(vaultKey: String, key: String) = b64.encodeToString(ShareCrypto.seal(vaultKey.toByteArray(), key))
    private fun unwrap(wrapped: String, key: String) = String(ShareCrypto.open(unb64.decode(wrapped), key))

    private suspend fun putVaultKey(session: String, wrapped: String, credentialId: String?) {
        val (code, body) = callJson("PUT", "/me/vault-key", JSONObject().put("wrapped", wrapped).apply { credentialId?.let { put("credentialId", it) } }, session)
        if (code != 204) fail(code, body)
    }

    /** Runs a passkey assertion with PRF; returns the server's answer, the credential ID and the PRF secret if any. */
    private suspend fun assert(activity: Activity): Triple<JSONObject, String, ByteArray?> {
        val c = challenge("login")
        val request = JSONObject().put("challenge", c.getString("challenge")).put("rpId", c.getString("rpId"))
            .put("timeout", 120_000).put("userVerification", "preferred").put("allowCredentials", JSONArray()).put("extensions", prfExtension())
        val result = try {
            CredentialManager.create(activity).getCredential(activity, GetCredentialRequest(listOf(GetPublicKeyCredentialOption(request.toString()))))
        } catch (e: GetCredentialCancellationException) { throw AccountException("Cancelled", cancelled = true) }
        catch (e: NoCredentialException) { throw AccountException("No Tempo passkey on this device. Create an account first.") }
        catch (e: Exception) { throw AccountException("Couldn't sign in: ${e.message ?: "try again"}") }
        val credential = JSONObject((result.credential as? PublicKeyCredential)?.authenticationResponseJson ?: throw AccountException("That wasn't a passkey"))
        val id = credential.getString("id")
        val (code, body) = callJson("POST", "/auth/login", JSONObject().put("challengeId", c.getString("challengeId")).put("credentialId", id)
            .put("response", credential.getJSONObject("response").put("id", id)))
        if (code != 200) fail(code, body)
        return Triple(JSONObject(body), id, prfResult(credential))
    }

    /** Gives a signed-in account a vault key: wrapped by the passkey's PRF secret, or by a new recovery code. */
    private suspend fun newVault(accountId: String, session: String, credentialId: String, prf: ByteArray?): AccountResult {
        val vaultKey = ShareCrypto.newKey()
        return if (prf != null) {
            putVaultKey(session, wrap(vaultKey, wrapKey(prf)), credentialId)
            AccountResult(SignedIn(accountId, session, vaultKey))
        } else {
            val code = newRecoveryCode()
            putVaultKey(session, wrap(vaultKey, recoveryKey(code, accountId)), null)
            AccountResult(SignedIn(accountId, session, vaultKey), recoveryCode = code)
        }
    }

    /** Creates a passkey and an account, with its vault key ready. [name] labels the passkey. */
    suspend fun create(activity: Activity, name: String): AccountResult {
        val c = challenge("register")
        val request = JSONObject()
            .put("challenge", c.getString("challenge"))
            .put("rp", JSONObject().put("id", c.getString("rpId")).put("name", "Tempo"))
            .put("user", JSONObject().put("id", c.getString("userId")).put("name", name.ifBlank { "Tempo" }).put("displayName", name.ifBlank { "Tempo" }))
            .put("pubKeyCredParams", JSONArray().put(JSONObject().put("type", "public-key").put("alg", -7)).put(JSONObject().put("type", "public-key").put("alg", -257)))
            .put("timeout", 120_000)
            .put("attestation", "none")
            .put("authenticatorSelection", JSONObject().put("residentKey", "required").put("requireResidentKey", true).put("userVerification", "preferred"))
            .put("extensions", prfExtension())
        val result = try {
            CredentialManager.create(activity).createCredential(activity, CreatePublicKeyCredentialRequest(request.toString())) as CreatePublicKeyCredentialResponse
        } catch (e: CreateCredentialCancellationException) { throw AccountException("Cancelled", cancelled = true) }
        catch (e: Exception) { throw AccountException("Couldn't create a passkey: ${e.message ?: "try again"}") }
        val credential = JSONObject(result.registrationResponseJson)
        val id = credential.getString("id")
        val (code, body) = callJson("POST", "/auth/register", JSONObject().put("challengeId", c.getString("challengeId")).put("response", credential.getJSONObject("response").put("id", id)))
        if (code != 201) fail(code, body)
        val json = JSONObject(body)
        val accountId = json.getString("accountId"); val session = json.getString("session")
        // Some providers only give the PRF secret when signing in, not when creating; ask once more if it supports PRF.
        var prf = prfResult(credential)
        val supportsPrf = credential.optJSONObject("clientExtensionResults")?.optJSONObject("prf")?.optBoolean("enabled") == true
        if (prf == null && supportsPrf) prf = runCatching { assert(activity).third }.getOrNull()
        return newVault(accountId, session, id, prf)
    }

    /** Signs in with a passkey and unlocks the vault key on this device when it can. */
    suspend fun signIn(activity: Activity): AccountResult {
        val (json, credentialId, prf) = assert(activity)
        val accountId = json.getString("accountId"); val session = json.getString("session")
        val wrapped = json.optString("wrappedVault").takeIf { it.isNotEmpty() && it != "null" }
        val recovery = json.optString("recoveryVault").takeIf { it.isNotEmpty() && it != "null" }
        if (wrapped != null && prf != null) return AccountResult(SignedIn(accountId, session, unwrap(wrapped, wrapKey(prf))))
        if (recovery != null) return AccountResult(SignedIn(accountId, session, null), needsRecoveryCode = recovery)
        // An account from before cloud saving: set its vault up now.
        return newVault(accountId, session, credentialId, prf)
    }

    /** Unlocks the vault key with the account's recovery code. */
    fun unlockWithCode(account: SignedIn, wrapped: String, code: String): SignedIn = try {
        account.copy(vaultKey = unwrap(wrapped, recoveryKey(code, account.accountId)))
    } catch (e: Exception) { throw AccountException("That recovery code doesn't match. Check it and try again.") }

    suspend fun saveProfile(account: SignedIn, profile: Profile) {
        val key = account.vaultKey ?: return
        val (code, body) = call("PUT", "/me/profile", bytes = ShareCrypto.seal(profile.toJson(), key), session = account.session)
        if (code != 204) fail(code, String(body))
    }

    /** IDs of links this account owns. */
    suspend fun ownedShares(account: SignedIn): Set<String> {
        val (code, body) = callJson("GET", "/me", session = account.session)
        if (code != 200) fail(code, body)
        val ids = JSONObject(body).getJSONArray("shares")
        return (0 until ids.length()).map { ids.getString(it) }.toSet()
    }

    /** The cloud copy: revision and ciphertext, or null when nothing is saved yet. */
    suspend fun getVault(account: SignedIn): Pair<Long, ByteArray>? {
        val (code, body, headers) = call("GET", "/me/vault", session = account.session)
        if (code == 404) return null
        if (code != 200) fail(code, String(body))
        return (headers.entries.firstOrNull { it.key.equals("x-tempo-rev", true) }?.value?.firstOrNull()?.toLongOrNull() ?: 0L) to body
    }
    /** Saves the cloud copy if [rev] is still the latest; returns the new revision, or null when another device saved first. */
    suspend fun putVault(account: SignedIn, rev: Long, body: ByteArray): Long? {
        val (code, response) = call("PUT", "/me/vault", bytes = body, session = account.session, headers = mapOf("X-Tempo-Rev" to rev.toString()))
        if (code == 412) return null
        if (code != 200) fail(code, String(response))
        return JSONObject(String(response)).getLong("rev")
    }

    /** Ends this device's session. */
    suspend fun signOut(account: SignedIn) { runCatching { call("DELETE", "/me/session", session = account.session) } }

    /** Deletes the account, its passkey record, sessions, profile, cloud copy and every link it owns. */
    suspend fun delete(account: SignedIn) {
        val (code, body) = call("DELETE", "/me", session = account.session)
        if (code != 204 && code != 401) fail(code, String(body))
    }
}
