package cc.xdan.tempo.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cc.xdan.tempo.model.Timetable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tempoStore by preferencesDataStore(name = "tempo_v1")

class TimetableRepository(context: Context) {
    private val store = context.applicationContext.tempoStore
    private val key = stringPreferencesKey("timetable")
    val timetable: Flow<Timetable> = store.data.map { prefs ->
        prefs[key]?.let(TimetableCodec::decode) ?: Timetable()
    }
    /** Decode and transform inside the transaction, so concurrent edits cannot lose data. */
    suspend fun update(transform: (Timetable) -> Timetable) {
        store.edit { prefs ->
            val previous = prefs[key]?.let(TimetableCodec::decode) ?: Timetable()
            prefs[key] = TimetableCodec.encode(transform(previous))
        }
    }
}
