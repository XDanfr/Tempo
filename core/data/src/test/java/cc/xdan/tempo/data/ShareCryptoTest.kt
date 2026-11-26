package cc.xdan.tempo.data

import cc.xdan.tempo.model.*
import org.junit.Assert.*
import org.junit.Test

class ShareCryptoTest {
    private val table = Timetable(name = "College", onboarded = true, subjects = listOf(Subject("m", "Maths", location = "Room 5")),
        sessions = listOf(Session("a", "m", 1, TimeSpan(540, 600), location = "Lab", notes = "Bring calculator")),
        checkIns = listOf(CheckIn("2026-11-23", "a")), person = null)

    @Test fun roundTripsAndRedactsByDefault() {
        val key = ShareCrypto.newKey()
        val shared = ShareCrypto.decrypt(ShareCrypto.encrypt("Alex", table, ShareOptions(), key), key)
        assertEquals("Alex", shared.sharer)
        assertEquals("College", shared.timetable.name)
        assertEquals("", shared.timetable.subjects.single().location)
        assertEquals("", shared.timetable.sessions.single().location)
        assertEquals("", shared.timetable.sessions.single().notes)
        assertTrue(shared.timetable.checkIns.isEmpty())
    }
    @Test fun includesRoomsAndNotesOnlyWhenChosen() {
        val key = ShareCrypto.newKey()
        val shared = ShareCrypto.decrypt(ShareCrypto.encrypt("Alex", table, ShareOptions(rooms = true, notes = true), key), key)
        assertEquals("Lab", shared.timetable.sessions.single().location)
        assertEquals("Bring calculator", shared.timetable.sessions.single().notes)
    }
    @Test fun wrongKeyOrTamperingFails() {
        val data = ShareCrypto.encrypt("Alex", table, ShareOptions(), ShareCrypto.newKey())
        assertThrows(IllegalArgumentException::class.java) { ShareCrypto.decrypt(data, ShareCrypto.newKey()) }
    }
    @Test fun parsesLinks() {
        val key = ShareCrypto.newKey()
        val link = ShareLink("AAAAAAAAAAAAAAAAAAAAAA", key)
        assertEquals(link, ShareLink.parse(link.url))
        assertEquals(link, ShareLink.parse("tempo://t/AAAAAAAAAAAAAAAAAAAAAA#k=$key"))
        assertNull(ShareLink.parse("https://evil.example/t/AAAAAAAAAAAAAAAAAAAAAA#k=$key"))
        assertNull(ShareLink.parse("https://tempo.xdan.cc/t/AAAAAAAAAAAAAAAAAAAAAA"))
    }
}
