package cc.xdan.tempo.model

import org.junit.Assert.*
import org.junit.Test

class SubjectIconSuggestionTest {
    @Test fun recognisedNamesIgnoreCaseAndExtraWhitespace() {
        assertEquals(SubjectIcon.MATHS, suggestedSubjectIcon("  MATHEMATICS "))
        assertEquals(SubjectIcon.CODE, suggestedSubjectIcon("Computer   Science"))
        assertEquals(SubjectIcon.LITERATURE, suggestedSubjectIcon("English literature"))
    }
    @Test fun unknownAndPartialNamesDoNotGuess() {
        for (name in listOf("", "Martial arts", "Music production meeting", "My activity")) assertEquals(SubjectIcon.NONE, suggestedSubjectIcon(name))
    }
}
