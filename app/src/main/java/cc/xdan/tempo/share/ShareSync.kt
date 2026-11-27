package cc.xdan.tempo.share

import android.content.Context
import androidx.work.*
import cc.xdan.tempo.data.ShareCrypto
import cc.xdan.tempo.data.ShareOptions
import cc.xdan.tempo.data.TimetableRepository
import cc.xdan.tempo.data.TimetableTransfer
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Keeps shared timetables up to date in both directions.
 *
 * Owners: after an edit settles, each shared timetable is re-encrypted and uploaded, but only when
 * the shared content actually changed. Followers: on app start and every few hours, each followed
 * link is checked with its revision; unchanged links cost one tiny request, changed ones replace
 * the local copy, and links the owner stopped sharing are removed from this device.
 */
object ShareSync {
    private const val PERIODIC = "tempo-share-sync"
    private const val SOON = "tempo-share-sync-soon"

    /** Runs a sync shortly, replacing any pending one so rapid edits upload once. */
    fun soon(context: Context, delaySeconds: Long = 30) {
        WorkManager.getInstance(context).enqueueUniqueWork(SOON, ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ShareSyncWorker>().setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
    }

    /** Background checks for followed timetables, only while there is something to sync. */
    fun ensureScheduled(context: Context) {
        val work = WorkManager.getInstance(context)
        if (ShareStore.followed(context).isEmpty() && ShareStore.allOwned(context).isEmpty()) { work.cancelUniqueWork(PERIODIC); return }
        work.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ShareSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
    }

    private fun sha256(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    /** What actually gets shared, so the hash changes only when the shared content changes. */
    fun contentHash(sharer: String, timetable: cc.xdan.tempo.model.Timetable, share: OwnedShare) =
        sha256(sharer + "\n" + TimetableTransfer.encode(ShareCrypto.redact(timetable, ShareOptions(share.rooms, share.notes))))

    suspend fun pushOwned(context: Context, repository: TimetableRepository) {
        val collection = repository.collection.first()
        ShareStore.allOwned(context).forEach { (timetableId, share) ->
            val saved = collection.timetables.firstOrNull { it.id == timetableId }
            try {
                if (saved == null) {
                    // The timetable was deleted: stop sharing it too.
                    ShareClient.delete(share.id, share.token); ShareStore.forget(context, timetableId); return@forEach
                }
                val sharer = collection.profileName
                val hash = contentHash(sharer, saved.timetable, share)
                if (hash == ShareStore.uploadedHash(context, timetableId)) return@forEach
                ShareClient.update(share.id, share.token, ShareCrypto.encrypt(sharer, saved.timetable, ShareOptions(share.rooms, share.notes), share.key))
                ShareStore.setUploadedHash(context, timetableId, hash)
            } catch (e: ShareException) {
                if (e.gone) ShareStore.forget(context, timetableId)
            }
        }
    }

    /** Returns the names of people whose timetables were removed because they stopped sharing. */
    suspend fun pullFollowed(context: Context, repository: TimetableRepository): List<String> {
        val removed = mutableListOf<String>()
        ShareStore.followed(context).forEach { (timetableId, followed) ->
            val local = repository.collection.first().timetables.firstOrNull { it.id == timetableId }
            if (local == null) { ShareStore.unfollow(context, timetableId); return@forEach }
            try {
                val (rev, data) = ShareClient.fetchIfChanged(followed.link.id, followed.rev) ?: return@forEach
                val shared = ShareCrypto.decrypt(data, followed.link.key)
                repository.updateCollection { collection ->
                    collection.update(timetableId) { old ->
                        // Their schedule, your labels: keep the person's name and the local name you see.
                        shared.timetable.copy(onboarded = true, person = old.person ?: shared.sharer.ifBlank { null }, name = old.name)
                    }
                }
                ShareStore.follow(context, timetableId, followed.link, rev)
            } catch (e: ShareException) {
                if (e.gone) {
                    removed += local.timetable.person ?: local.timetable.name
                    repository.updateCollection { collection ->
                        if (collection.timetables.size > 1) collection.remove(timetableId) else collection
                    }
                    ShareStore.unfollow(context, timetableId)
                }
            } catch (e: IllegalArgumentException) { /* Damaged or wrong key: leave the copy as it is. */ }
        }
        return removed
    }
}

class ShareSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val repository = TimetableRepository(applicationContext)
        ShareSync.pushOwned(applicationContext, repository)
        val removed = ShareSync.pullFollowed(applicationContext, repository)
        if (removed.isNotEmpty()) applicationContext.getSharedPreferences("shares", Context.MODE_PRIVATE).edit()
            .putString("removedNotice", removed.joinToString(", ")).apply()
        Result.success()
    } catch (e: kotlinx.coroutines.CancellationException) { throw e }
    catch (e: Exception) { Result.retry() }
}
