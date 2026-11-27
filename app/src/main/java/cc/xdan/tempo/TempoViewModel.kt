package cc.xdan.tempo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.xdan.tempo.data.TimetableRepository
import cc.xdan.tempo.data.TimetableTransfer
import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import cc.xdan.tempo.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ImportPreview(val timetable: Timetable, val targetId: String)

/**
 * One piece of user-facing feedback, emitted only after an action has completed.
 * [revision] binds Undo/Redo to the history state the person saw, so a late tap never reverses
 * a different change.
 */
sealed interface Feedback {
    val id: Long
    /** A named edit was saved and can be undone. */
    data class Changed(override val id: Long, val label: String, val revision: Int) : Feedback
    /** An edit was undone and can be redone. */
    data class Undone(override val id: Long, val label: String, val revision: Int) : Feedback
    /** Plain information with no action, such as a finished export. */
    data class Info(override val id: Long, val text: String) : Feedback
}

data class TempoState(val collection: TimetableCollection? = null, val error: String? = null,
    val importPreview: ImportPreview? = null, val contentEpoch: Int = 0, val fileBusy: Boolean = false,
    val feedback: Feedback? = null, val canUndo: Boolean = false, val canRedo: Boolean = false,
    val undoLabel: String? = null, val redoLabel: String? = null,
    val sharePreview: cc.xdan.tempo.data.SharedTimetable? = null, val openingShare: Boolean = false,
    val shareLink: cc.xdan.tempo.data.ShareLink? = null) {
    val timetable: Timetable? get() = collection?.active
}

class TempoViewModel(private val repository: TimetableRepository) : ViewModel() {
    private val editLock = Mutex()
    private val history = ChangeHistory<TimetableCollection>()
    private var feedbackId = 0L
    private val mutable = MutableStateFlow(TempoState())
    val state = mutable.asStateFlow()
    init {
        viewModelScope.launch {
            try { repository.collection.collect { mutable.value = mutable.value.copy(collection = it) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { showError("Could not read your timetables. Your saved data has been kept. " + (e.message ?: "Please restart Tempo.")) }
        }
    }
    /**
     * Saves a change to the active timetable. A [label] such as "Added Physics" shows an
     * undoable snackbar once the change is stored; unlabelled changes (settings toggles,
     * onboarding steps) stay undoable from the menu without interrupting.
     */
    fun update(label: String?, transform: (Timetable) -> Timetable) {
        val id = mutable.value.collection?.activeId ?: return
        save { changeCollection(label) { it.update(id, transform) } }
    }
    fun update(transform: (Timetable) -> Timetable) = update(null, transform)
    fun select(id: String) = save { changeCollection(null) { it.select(id) } }
    fun create(name: String) = save {
        changeCollection("Created ${name.trim()}") { it.add(UUID.randomUUID().toString(), Timetable(name = name.trim())) }
    }
    fun rename(id: String, name: String) = save { changeCollection("Renamed to ${name.trim()}") { it.update(id) { t -> t.copy(name = name.trim()) } } }
    fun duplicate(id: String) = save {
        val source = mutable.value.collection?.timetables?.firstOrNull { it.id == id }?.timetable?.name ?: "timetable"
        changeCollection("Duplicated $source") { collection ->
            val timetable = collection.timetables.first { it.id == id }.timetable
            collection.add(UUID.randomUUID().toString(), timetable.copy(name = uniqueName(timetable.name + " copy", collection)))
        }
    }
    fun setProfileName(name: String) = save { changeCollection(null) { it.copy(profileName = name.trim().take(40)) } }
    /** Marks a saved timetable as someone else's ([name]) or as yours (null). */
    fun setPerson(id: String, name: String?) = save {
        changeCollection(if (name.isNullOrBlank()) "Marked as yours" else "Marked as ${name.trim()}'s") {
            it.update(id) { t -> t.copy(person = name?.trim()?.takeIf(String::isNotEmpty)?.take(40)) }
        }
    }
    /** Opens a shared link: downloads the ciphertext and decrypts it with the key from the link. */
    fun openShareLink(text: String) {
        val link = cc.xdan.tempo.data.ShareLink.parse(text) ?: return showError("That isn't a complete Tempo share link. Ask for the full link, including the part after #.")
        mutable.value = mutable.value.copy(openingShare = true)
        viewModelScope.launch {
            try {
                val data = cc.xdan.tempo.share.ShareClient.fetch(link.id)
                val shared = withContext(Dispatchers.Default) { cc.xdan.tempo.data.ShareCrypto.decrypt(data, link.key) }
                mutable.value = mutable.value.copy(sharePreview = shared, shareLink = link)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { showError(e.message ?: "Couldn't open this shared timetable.") }
            finally { mutable.value = mutable.value.copy(openingShare = false) }
        }
    }
    fun dismissSharePreview() { mutable.value = mutable.value.copy(sharePreview = null) }
    /** Adds the previewed share as someone's timetable (for frees) or as an independent copy of your own. */
    fun acceptShare(asPerson: Boolean, follow: (timetableId: String, link: cc.xdan.tempo.data.ShareLink) -> Unit = { _, _ -> }) {
        val shared = mutable.value.sharePreview ?: return
        val link = mutable.value.shareLink
        val newId = UUID.randomUUID().toString()
        dismissSharePreview()
        val name = shared.sharer.ifBlank { "Friend" }
        save {
            changeCollection(if (asPerson) "Added $name" else "Imported ${shared.timetable.name}") { collection ->
                val copy = shared.timetable.copy(onboarded = true, person = if (asPerson) name else null)
                collection.add(newId, copy.copy(name = uniqueName(if (asPerson) "$name · ${copy.name}" else copy.name, collection)))
                    .select(collection.activeId)
            }
            // People's timetables stay in step with the owner; imported copies are independent.
            if (asPerson && link != null) follow(newId, link)
        }
    }
    fun delete(id: String) = save {
        val name = mutable.value.collection?.timetables?.firstOrNull { it.id == id }?.timetable?.name ?: "timetable"
        changeCollection("Deleted $name") { it.remove(id) }
    }
    fun importTimetable(timetable: Timetable, replaceId: String?) = save {
        changeCollection(if (replaceId != null) "Replaced with ${timetable.name}" else "Imported ${timetable.name}") { collection ->
            val ready = timetable.copy(onboarded = true, onboardingStep = 3)
            if (replaceId != null) collection.update(replaceId) { ready }.select(replaceId)
            else collection.add(UUID.randomUUID().toString(), ready.copy(name = uniqueName(ready.name, collection)))
        }
        mutable.value = mutable.value.copy(contentEpoch = mutable.value.contentEpoch + 1)
    }
    private fun uniqueName(name: String, collection: TimetableCollection): String {
        val names = collection.timetables.map { it.timetable.name }.toSet()
        if (name !in names) return name
        var suffix = 2
        while ("$name ($suffix)" in names) suffix++
        return "$name ($suffix)"
    }
    private fun save(action: suspend () -> Unit) {
        viewModelScope.launch {
            try { editLock.withLock { action() } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { showError("Could not save this change: " + (e.message ?: "Try again.")) }
        }
    }
    private suspend fun changeCollection(label: String?, transform: (TimetableCollection) -> TimetableCollection) {
        var before: TimetableCollection? = null
        var after: TimetableCollection? = null
        repository.updateCollection { old -> transform(old).also { before = old; after = it } }
        // Feedback is emitted only after storage succeeded, and never for unchanged values.
        val changed = history.record(before!!, after!!, label)
        publishHistory(if (changed && label != null) Feedback.Changed(++feedbackId, label, history.revision) else null)
    }
    private fun publishHistory(feedback: Feedback? = null) {
        mutable.value = mutable.value.copy(canUndo = history.canUndo, canRedo = history.canRedo,
            undoLabel = history.undoLabel, redoLabel = history.redoLabel,
            feedback = feedback ?: mutable.value.feedback)
    }
    /**
     * Undoes the latest change. When [expectedRevision] is given (from a snackbar), nothing
     * happens unless history is still exactly as that snackbar described it.
     */
    fun undo(expectedRevision: Int?) = save {
        if (expectedRevision != null && expectedRevision != history.revision) return@save
        val target = history.undoTarget() ?: return@save
        var current: TimetableCollection? = null
        repository.updateCollection { current = it; target }
        val label = history.didUndo(current!!)
        publishHistory(Feedback.Undone(++feedbackId, label ?: "last change", history.revision))
    }
    fun undo() = undo(null)
    /** Redoes the latest undone change, with the same revision guard as [undo]. */
    fun redo(expectedRevision: Int?) = save {
        if (expectedRevision != null && expectedRevision != history.revision) return@save
        val target = history.redoTarget() ?: return@save
        var current: TimetableCollection? = null
        repository.updateCollection { current = it; target }
        val label = history.didRedo(current!!)
        publishHistory(Feedback.Changed(++feedbackId, label ?: "Redid last change", history.revision))
    }
    fun redo() = redo(null)
    private var importTargetId: String? = null
    private var exportSnapshot: Timetable? = null
    fun beginImport() { importTargetId = mutable.value.collection?.activeId }
    fun readImport(resolver: ContentResolver, uri: Uri?) {
        if (uri == null) return
        val target = importTargetId ?: mutable.value.collection?.activeId ?: return
        mutable.value = mutable.value.copy(fileBusy = true)
        viewModelScope.launch {
            try {
                val timetable = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use(TimetableTransfer::read) ?: error("Could not open the selected file")
                }
                mutable.value = mutable.value.copy(importPreview = ImportPreview(timetable, target))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { showError("Could not import this timetable. " + (e.message ?: "Choose a valid Tempo file.")) }
            finally { mutable.value = mutable.value.copy(fileBusy = false) }
        }
    }
    fun dismissImport() { mutable.value = mutable.value.copy(importPreview = null) }
    fun acceptImport(replace: Boolean) {
        val preview = mutable.value.importPreview ?: return
        importTimetable(preview.timetable, if (replace) preview.targetId else null)
        dismissImport()
    }
    fun beginExport(): String? {
        val timetable = mutable.value.timetable ?: return null
        exportSnapshot = timetable
        return timetable.name.replace(Regex("[^\\p{L}\\p{N} ._-]"), "_").take(80).trim().ifBlank { "Timetable" } + ".tempo.json"
    }
    fun writeExport(resolver: ContentResolver, uri: Uri?) {
        if (uri == null) { exportSnapshot = null; return }
        val snapshot = exportSnapshot
        exportSnapshot = null
        if (snapshot == null) { showError("Export was interrupted. Please export again."); return }
        mutable.value = mutable.value.copy(fileBusy = true)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val bytes = TimetableTransfer.encode(snapshot).toByteArray(Charsets.UTF_8)
                    require(bytes.size <= TimetableTransfer.MAX_BYTES) { "This timetable exceeds the 2 MB file limit" }
                    resolver.openOutputStream(uri, "wt")?.use { it.write(bytes) } ?: error("Could not write to the chosen location")
                }
                info("Exported ${snapshot.name}")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { showError("Could not export your timetable. " + (e.message ?: "Try another location.")) }
            finally { mutable.value = mutable.value.copy(fileBusy = false) }
        }
    }
    /** Clears feedback only if it is still the one that was shown, so newer feedback survives. */
    fun dismissFeedback(id: Long) {
        if (mutable.value.feedback?.id == id) mutable.value = mutable.value.copy(feedback = null)
    }
    fun info(text: String) { mutable.value = mutable.value.copy(feedback = Feedback.Info(++feedbackId, text)) }
    fun showMessage(message: String) = info(message)
    fun showError(message: String) { mutable.value = mutable.value.copy(error = message) }
    fun dismissError() { mutable.value = mutable.value.copy(error = null) }
}
