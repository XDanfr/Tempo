package cc.xdan.tempo

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cc.xdan.tempo.data.TimetableRepository
import cc.xdan.tempo.design.TempoTheme
import cc.xdan.tempo.model.*
import cc.xdan.tempo.ui.*
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditingIntegrationTest {
    @get:Rule val compose = createComposeRule()
    private val table = Timetable(onboarded = true, subjects = listOf(Subject("s", "Maths")),
        sessions = listOf(Session("a", "s", 1, TimeSpan(540, 600))))

    @Test fun batchSelectionAppliesOnlyToSelectedSessions() {
        val value = mutableStateOf(table.copy(sessions = table.sessions + table.sessions.single().copy(id = "b", day = 2)))
        compose.setContent { TempoTheme(value.value.appearance) {
            BatchSessionEditor(value.value, { value.value = it(value.value) }, {})
        } }
        compose.onNodeWithText("Maths · Mon 09:00 · every week").performClick()
        compose.onNodeWithText("Shift by minutes (negative moves earlier)").performScrollTo().performTextReplacement("30")
        compose.onNodeWithText("Apply").performClick()
        compose.waitUntil(10_000) { value.value.sessions.first().time.start == 570 }
        assertEquals(540, value.value.sessions[1].time.start)
    }

    @Test fun breakCycleChoiceIsPersisted() {
        val value = mutableStateOf<ScheduledBreak?>(null)
        compose.setContent { TempoTheme(table.appearance) {
            BreakEditor(table.copy(cycle = WeekCycle(2)), initialDay = 1, dismiss = {}, save = { value.value = it }, delete = {})
        } }
        compose.onNodeWithText("Cycle week").performScrollTo().performClick()
        compose.onNodeWithText("Week 2").performClick()
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(10_000) { value.value != null }
        assertEquals(2, value.value!!.cycleWeek)
    }

    @Test fun appUndoRedoRestoresSavedDataAndFailedEditsKeepHistory() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = TimetableRepository(context)
        val fixture = table.copy(sessions = table.sessions.map { it.copy(day = LocalDate.now().dayOfWeek.value) })
        runBlocking { repository.updateCollection { TimetableCollection(timetables = listOf(SavedTimetable("local", fixture))) } }
        val model = TempoViewModel(repository)
        val store = ViewModelStore().also { it.put("qa", model) }
        try {
            compose.setContent { TempoApp(model) }
            compose.waitUntil(10_000) { model.state.value.timetable != null }
            model.update { it.copy(name = "Changed") }
            compose.waitUntil(10_000) { model.state.value.timetable?.name == "Changed" && model.state.value.canUndo }
            compose.onNodeWithContentDescription("Undo edit").performClick()
            compose.waitUntil(10_000) { model.state.value.timetable?.name == fixture.name && model.state.value.canRedo }
            model.update { it.copy(name = "") }
            compose.waitUntil(10_000) { model.state.value.error != null }
            model.dismissError()
            compose.onNodeWithContentDescription("Redo edit").performClick()
            compose.waitUntil(10_000) { model.state.value.timetable?.name == "Changed" && !model.state.value.canRedo }
            assertFalse(model.state.value.canRedo)
            runBlocking { repository.updateCollection { current -> assertEquals("Changed", current.active.name); current } }
        } finally { compose.runOnIdle { store.clear() } }
    }

    @Test fun comparisonDateNavigationKeepsBothModesWorking() {
        val collection = TimetableCollection(timetables = listOf(SavedTimetable("local", table))).add("other", table.copy(name = "Other", cycle = WeekCycle(2)))
        compose.setContent { TempoTheme(table.appearance) { ComparisonScreen(collection, {}) } }
        compose.onNodeWithContentDescription("Next week").performClick()
        compose.onNodeWithText("Together").performClick()
        compose.onNodeWithText("Side by side").performClick()
        compose.onNodeWithText("Choose").performClick()
        compose.onNodeWithText("Compare").performClick()
        compose.onNodeWithText("2 timetables").assertExists()
    }
}
