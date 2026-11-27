package cc.xdan.tempo.account

import android.content.Context
import cc.xdan.tempo.data.ShareCrypto
import cc.xdan.tempo.data.TimetableRepository
import cc.xdan.tempo.share.ShareClient
import cc.xdan.tempo.share.ShareStore
import java.util.UUID

/**
 * Friends' profile keys (needed to read their profiles), and which local timetable shows each friend's shared
 * timetable. Part of the encrypted cloud copy so friends come with you to a new phone.
 */
object FriendStore {
    private fun prefs(context: Context) = context.getSharedPreferences("friends", Context.MODE_PRIVATE)
    fun keys(context: Context): Map<String, String> = prefs(context).all.filterKeys { it.startsWith("key:") }
        .mapKeys { it.key.removePrefix("key:") }.mapValues { it.value as String }
    fun saveKey(context: Context, accountId: String, key: String) = prefs(context).edit().putString("key:$accountId", key).apply()
    fun timetableFor(context: Context, accountId: String): String? = prefs(context).getString("tt:$accountId", null)
    fun forget(context: Context, accountId: String) = prefs(context).edit().remove("key:$accountId").remove("tt:$accountId").apply()
    /** Whether your shared timetable is offered to friends, and which one. */
    fun sharedTimetable(context: Context): String? = prefs(context).getString("mine", null)
    fun setSharedTimetable(context: Context, timetableId: String?) = prefs(context).edit().putString("mine", timetableId).apply()

    fun exportRaw(context: Context): Map<String, String> = prefs(context).all.filterValues { it is String }.mapValues { it.value as String }
    fun importRaw(context: Context, entries: Map<String, String>) {
        val p = prefs(context)
        p.edit().apply { entries.filterKeys { !p.contains(it) }.forEach { (k, v) -> putString(k, v) } }.apply()
    }

    /**
     * Adds each friend's shared timetable as a person, following their link, and removes timetables of people
     * who are no longer friends. Their updates then arrive through the normal share sync.
     */
    suspend fun syncTimetables(context: Context, friends: List<Accounts.Person>) {
        val repository = TimetableRepository(context)
        val current = friends.map { it.accountId }.toSet()
        // No longer friends: remove the timetable this device added for them.
        prefs(context).all.keys.filter { it.startsWith("tt:") }.map { it.removePrefix("tt:") }.filter { it !in current }.forEach { gone ->
            timetableFor(context, gone)?.let { id -> repository.updateCollection { c -> if (c.timetables.size > 1) c.remove(id) else c }; ShareStore.unfollow(context, id) }
            prefs(context).edit().remove("tt:$gone").apply()
        }
        for (friend in friends) {
            val link = friend.profile?.link ?: continue
            val existing = timetableFor(context, friend.accountId)
            val followed = ShareStore.followed(context)
            if (existing != null && followed[existing]?.link?.id == link.id) continue
            val shared = runCatching { ShareCrypto.decrypt(ShareClient.fetch(link.id), link.key) }.getOrNull() ?: continue
            val name = friend.profile.name.ifBlank { shared.sharer.ifBlank { "Friend" } }
            val id = existing ?: UUID.randomUUID().toString()
            repository.updateCollection { c ->
                val copy = shared.timetable.copy(onboarded = true, person = name, name = "$name · ${shared.timetable.name}")
                if (c.timetables.any { it.id == id }) c.update(id) { copy } else c.add(id, copy).select(c.activeId)
            }
            ShareStore.follow(context, id, link)
            prefs(context).edit().putString("tt:${friend.accountId}", id).apply()
        }
    }
}
