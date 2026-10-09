package cc.xdan.tempo.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cc.xdan.tempo.model.Timetable
import cc.xdan.tempo.model.TimetableCollection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tempoStore by preferencesDataStore(name = "tempo_v1")

class TimetableRepository(context: Context) {
    private val store = context.applicationContext.tempoStore
    // Reuse the old key: its single snapshot is migrated inside the first write transaction.
    private val key = stringPreferencesKey("timetable")
    val collection: Flow<TimetableCollection> = store.data.map { prefs ->
        prefs[key]?.let(TimetableCollectionCodec::decode) ?: TimetableCollection()
    }
    suspend fun updateTimetable(id: String, transform: (Timetable) -> Timetable) =
        updateCollection { it.update(id, transform) }

    /** Decode and transform inside the transaction, so concurrent edits cannot lose data. */
    suspend fun updateCollection(transform: (TimetableCollection) -> TimetableCollection) {
        store.edit { prefs ->
            val previous = prefs[key]?.let(TimetableCollectionCodec::decode) ?: TimetableCollection()
            prefs[key] = TimetableCollectionCodec.encode(transform(previous))
        }
    }
}
