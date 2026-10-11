package cc.xdan.tempo.model

/** Session-local snapshots are bounded; redo is discarded only after a successful new edit. */
class EditHistory<T>(private val limit: Int = 50) {
    init { require(limit > 0) }
    private val past = ArrayDeque<T>()
    private val future = ArrayDeque<T>()
    val canUndo get() = past.isNotEmpty()
    val canRedo get() = future.isNotEmpty()
    fun record(before: T, after: T) {
        if (before == after) return
        past.addLast(before)
        while (past.size > limit) past.removeFirst()
        future.clear()
    }
    fun undoTarget(): T? = past.lastOrNull()
    fun redoTarget(): T? = future.lastOrNull()
    fun didUndo(current: T) { check(canUndo); past.removeLast(); future.addLast(current) }
    fun didRedo(current: T) { check(canRedo); future.removeLast(); past.addLast(current) }
}
