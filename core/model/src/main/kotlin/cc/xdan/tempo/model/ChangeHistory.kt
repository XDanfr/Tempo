package cc.xdan.tempo.model

/**
 * Bounded undo/redo history that remembers what each change was called and counts revisions,
 * so feedback offered for one change ("Undo") can never reverse a different, later change.
 */
class ChangeHistory<T>(limit: Int = 50) {
    private data class Entry<T>(val state: T, val label: String?)
    private val history = EditHistory<Entry<T>>(limit)
    private var currentLabel: String? = null

    /** Increases every time history changes; feedback binds its action to the value it saw. */
    var revision = 0
        private set
    val canUndo get() = history.canUndo
    val canRedo get() = history.canRedo
    /** Name of the change Undo would reverse, if it was named. */
    val undoLabel: String? get() = if (canUndo) currentLabel else null
    /** Name of the change Redo would reapply, if it was named. */
    val redoLabel: String? get() = history.redoTarget()?.label

    /** Records a committed change. Returns false (and records nothing) when nothing changed. */
    fun record(before: T, after: T, label: String?): Boolean {
        if (before == after) return false
        history.record(Entry(before, currentLabel), Entry(after, label))
        currentLabel = label
        revision++
        return true
    }

    fun undoTarget(): T? = history.undoTarget()?.state
    fun redoTarget(): T? = history.redoTarget()?.state

    /** Completes an undo; returns the label of the change that was reversed. */
    fun didUndo(current: T): String? {
        val undone = currentLabel
        val target = history.undoTarget() ?: error("Nothing to undo")
        history.didUndo(Entry(current, currentLabel))
        currentLabel = target.label
        revision++
        return undone
    }

    /** Completes a redo; returns the label of the change that was reapplied. */
    fun didRedo(current: T): String? {
        val target = history.redoTarget() ?: error("Nothing to redo")
        history.didRedo(Entry(current, currentLabel))
        currentLabel = target.label
        revision++
        return target.label
    }
}
