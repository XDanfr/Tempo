package cc.xdan.tempo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.xdan.tempo.data.TimetableRepository
import cc.xdan.tempo.model.Timetable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TempoState(val timetable: Timetable? = null, val error: String? = null)

class TempoViewModel(private val repository: TimetableRepository) : ViewModel() {
    private val mutable = MutableStateFlow(TempoState())
    val state = mutable.asStateFlow()
    init { load() }
    private fun load() {
        viewModelScope.launch {
            try { repository.timetable.collect { mutable.value = TempoState(it) } }
            catch (e: Exception) { mutable.value = mutable.value.copy(error = "Could not read your timetable. Your saved data has been kept. " + (e.message ?: "Please restart Tempo.")) }
        }
    }
    fun update(transform: (Timetable) -> Timetable) {
        viewModelScope.launch {
            try { repository.update(transform) }
            catch (e: Exception) { mutable.value = mutable.value.copy(error = "Could not save this change: " + (e.message ?: "Try again.")) }
        }
    }
    fun dismissError() { mutable.value = mutable.value.copy(error = null) }
}
