package cc.xdan.tempo.model

import org.junit.Assert.*
import org.junit.Test

class ChangeHistoryTest {
    @Test fun labelsFollowUndoAndRedo() {
        val history = ChangeHistory<Int>()
        history.record(0, 1, "Added Physics")
        history.record(1, 2, "Updated session")
        assertEquals("Updated session", history.undoLabel)
        assertEquals(1, history.undoTarget())
        assertEquals("Updated session", history.didUndo(2))
        assertEquals("Added Physics", history.undoLabel)
        assertEquals("Updated session", history.redoLabel)
        assertEquals(2, history.redoTarget())
        assertEquals("Updated session", history.didRedo(1))
        assertEquals("Updated session", history.undoLabel)
        assertNull(history.redoLabel)
    }

    @Test fun revisionChangesOnEveryHistoryChangeButNotOnNoOps() {
        val history = ChangeHistory<String>()
        val start = history.revision
        assertTrue(history.record("a", "b", "First"))
        val afterFirst = history.revision
        assertFalse(history.record("b", "b", "Nothing"))
        assertEquals(afterFirst, history.revision)
        history.didUndo("b")
        assertTrue(history.revision > afterFirst)
        assertTrue(afterFirst > start)
    }

    @Test fun unnamedChangesStayUndoableWithoutLabels() {
        val history = ChangeHistory<Int>()
        history.record(0, 1, null)
        assertTrue(history.canUndo)
        assertNull(history.undoLabel)
        assertNull(history.didUndo(1))
        assertFalse(history.canUndo)
    }

    @Test fun newChangeAfterUndoClearsRedo() {
        val history = ChangeHistory<Int>()
        history.record(0, 1, "One")
        history.didUndo(1)
        history.record(0, 5, "Five")
        assertFalse(history.canRedo)
        assertEquals("Five", history.undoLabel)
    }
}
