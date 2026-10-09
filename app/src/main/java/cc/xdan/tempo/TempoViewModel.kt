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

data class ImportPreview(val timetable: Timetable, val targetId: String)

data class TempoState(val collection: TimetableCollection? = null, val error: String? = null,
    val importPreview: ImportPreview? = null, val fileBusy: Boolean = false, val message: String? = null) {
    val timetable: Timetable? get() = collection?.active
}

class TempoViewModel(private val repository: TimetableRepository) : ViewModel() {
    private val mutable = MutableStateFlow(TempoState())
    val state = mutable.asStateFlow()
    init {
        viewModelScope.launch {
            try { repository.collection.collect { mutable.value = mutable.value.copy(collection = it) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { showError("Could not read your timetables. Your saved data has been kept. " + (e.message ?: "Please restart Tempo.")) }
        }
    }
    fun update(transform: (Timetable) -> Timetable) {
        val id = mutable.value.collection?.activeId ?: return
        save { repository.updateTimetable(id, transform) }
    }
    fun select(id: String) = save { repository.updateCollection { it.select(id) } }
    fun create(name: String) = save {
        repository.updateCollection { it.add(UUID.randomUUID().toString(), Timetable(name = name.trim())) }
    }
    fun rename(id: String, name: String) = save { repository.updateTimetable(id) { it.copy(name = name.trim()) } }
    fun duplicate(id: String) = save {
        repository.updateCollection { collection ->
            val timetable = collection.timetables.first { it.id == id }.timetable
            collection.add(UUID.randomUUID().toString(), timetable.copy(name = uniqueName(timetable.name + " copy", collection)))
        }
    }
    fun delete(id: String) = save { repository.updateCollection { it.remove(id) } }
    fun importTimetable(timetable: Timetable, replaceId: String?) = save {
        repository.updateCollection { collection ->
            val ready = timetable.copy(onboarded = true, onboardingStep = 3)
            if (replaceId != null) collection.update(replaceId) { ready }.select(replaceId)
            else collection.add(UUID.randomUUID().toString(), ready.copy(name = uniqueName(ready.name, collection)))
        }
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
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { showError("Could not save this change: " + (e.message ?: "Try again.")) }
        }
    }
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
                mutable.value = mutable.value.copy(message = "Exported ${snapshot.name}")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { showError("Could not export your timetable. " + (e.message ?: "Try another location.")) }
            finally { mutable.value = mutable.value.copy(fileBusy = false) }
        }
    }
    fun dismissMessage() { mutable.value = mutable.value.copy(message = null) }
    fun showError(message: String) { mutable.value = mutable.value.copy(error = message) }
    fun dismissError() { mutable.value = mutable.value.copy(error = null) }
}
