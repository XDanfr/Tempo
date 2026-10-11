package cc.xdan.tempo.model

import org.junit.Assert.*
import org.junit.Test

class EditHistoryTest {
    @Test fun undoRedoAndBranchingAreBounded() {
        val history = EditHistory<Int>(2)
        history.record(0, 1); history.record(1, 2); history.record(2, 3)
        assertEquals(2, history.undoTarget())
        history.didUndo(3)
        assertEquals(3, history.redoTarget())
        history.didRedo(2)
        history.didUndo(3); history.didUndo(2)
        assertFalse(history.canUndo)
        history.record(1, 4)
        assertFalse(history.canRedo)
        assertEquals(1, history.undoTarget())
    }
    @Test fun noOpRetainsRedoAndPeekingDoesNotConsumeHistory() {
        val history = EditHistory<String>()
        history.record("old", "new")
        assertEquals("old", history.undoTarget())
        assertEquals("old", history.undoTarget())
        history.didUndo("new")
        history.record("old", "old")
        assertEquals("new", history.redoTarget())
    }
}
