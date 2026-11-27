package cc.xdan.tempo.account

import android.content.Context
import cc.xdan.tempo.data.ShareCrypto
import cc.xdan.tempo.data.TimetableCollectionCodec
import cc.xdan.tempo.data.TimetableRepository
import cc.xdan.tempo.model.TimetableCollection
import cc.xdan.tempo.share.ShareStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Encrypted cloud copy of every timetable, for signed-in accounts. The payload is the whole collection plus the
 * device's share links (owner tokens and keys), so sharing keeps working on another device. Encrypted with the
 * vault key, which the server never sees.
 */
object CloudSync {
    private const val FORMAT = "cc.xdan.tempo.vault"
    private val lock = Mutex()
    private fun prefs(context: Context) = context.getSharedPreferences("cloud", Context.MODE_PRIVATE)
    private fun hash(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    /** When the cloud copy was last brought up to date, or 0. */
    fun lastSynced(context: Context): Long = prefs(context).getLong("syncedAt", 0)

    /**
     * Union by timetable ID. Where both sides have a timetable, [preferLocal] decides; the local selection is
     * kept when it still exists. A timetable deleted on another device can come back if this one also changed.
     */
    fun merge(remote: TimetableCollection, local: TimetableCollection, preferLocal: Boolean): TimetableCollection {
        val byId = LinkedHashMap<String, cc.xdan.tempo.model.SavedTimetable>()
        remote.timetables.forEach { byId[it.id] = it }
        local.timetables.forEach { if (preferLocal || it.id !in byId) byId[it.id] = it }
        val all = byId.values.toList()
        val active = local.activeId.takeIf { id -> all.any { it.id == id } && local.timetables.firstOrNull { it.id == id }?.timetable?.onboarded == true }
            ?: remote.activeId.takeIf { id -> all.any { it.id == id } } ?: all.firstOrNull()?.id ?: local.activeId
        return remote.copy(timetables = all, activeId = active, profileName = local.profileName.ifBlank { remote.profileName })
    }

    /**
     * Pulls newer changes from the cloud, merges them in, then pushes this device's changes. Safe to call often:
     * nothing is uploaded when nothing changed. Returns true when the collection was replaced from the cloud.
     */
    suspend fun sync(context: Context): Boolean = lock.withLock {
        val account = AccountStore.current(context)?.takeIf { it.vaultKey != null } ?: return@withLock false
        val key = account.vaultKey!!
        val repository = TimetableRepository(context)
        val prefs = prefs(context)
        var rev = prefs.getLong("rev", 0)
        var changed = false
        suspend fun pull(): Boolean {
            val (remoteRev, body) = Accounts.getVault(account) ?: return false
            if (remoteRev <= rev) return false
            val root = JSONObject(String(ShareCrypto.open(body, key)))
            require(root.optString("format") == FORMAT) { "Unknown cloud format" }
            val remote = TimetableCollectionCodec.decode(root.getString("collection"))
            val savedHash = prefs.getString("hash", null)
            repository.updateCollection { local ->
                val localDirty = hash(TimetableCollectionCodec.encode(local)) != savedHash
                val nothingLocal = local.timetables.none { it.timetable.onboarded }
                // Untouched or empty here: take the cloud copy; otherwise keep both, local edits winning.
                when {
                    // A blank setup here is replaced outright, so no empty timetable is left behind.
                    nothingLocal -> remote.copy(profileName = local.profileName.ifBlank { remote.profileName })
                    !localDirty -> merge(remote, local, preferLocal = false)
                    else -> merge(remote, local, preferLocal = true)
                }
            }
            root.optJSONObject("friends")?.let { f -> FriendStore.importRaw(context, f.keys().asSequence().associateWith { f.getString(it) }) }
            root.optJSONObject("shares")?.let { shares -> ShareStore.importRaw(context, shares.keys().asSequence().associateWith { shares.getString(it) }) }
            rev = remoteRev
            prefs.edit().putLong("rev", rev).apply()
            changed = true
            return true
        }
        pull()
        // Push: whatever is here now, if it differs from what was last saved.
        repeat(2) {
            val current = repository.collection.first()
            if (current.timetables.none { it.timetable.onboarded }) return@withLock changed
            val encoded = TimetableCollectionCodec.encode(current)
            val currentHash = hash(encoded)
            // Links and friends travel in the same payload; a change to either also needs saving.
            val extrasHash = hash(ShareStore.exportRaw(context).toSortedMap().toString() + FriendStore.exportRaw(context).toSortedMap().toString())
            if (currentHash == prefs.getString("hash", null) && extrasHash == prefs.getString("extras", null)) { prefs.edit().putLong("syncedAt", System.currentTimeMillis()).apply(); return@withLock changed }
            val payload = JSONObject().put("format", FORMAT).put("formatVersion", 1).put("collection", encoded)
                .put("shares", JSONObject(ShareStore.exportRaw(context) as Map<*, *>))
                .put("friends", JSONObject(FriendStore.exportRaw(context) as Map<*, *>)).toString().toByteArray()
            val newRev = Accounts.putVault(account, rev, ShareCrypto.seal(payload, key))
            if (newRev != null) {
                prefs.edit().putLong("rev", newRev).putString("hash", currentHash).putString("extras", extrasHash).putLong("syncedAt", System.currentTimeMillis()).apply()
                return@withLock changed
            }
            // Another device saved first: bring its changes in, then try once more.
            pull()
        }
        changed
    }
}
