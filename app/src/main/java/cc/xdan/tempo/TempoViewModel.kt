package cc.xdan.tempo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.xdan.tempo.data.TimetableRepository
import cc.xdan.tempo.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.util.UUID

data class TempoState(val collection: TimetableCollection? = null, val error: String? = null) {
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
    fun showError(message: String) { mutable.value = mutable.value.copy(error = message) }
    fun dismissError() { mutable.value = mutable.value.copy(error = null) }
}
