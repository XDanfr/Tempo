package cc.xdan.tempo.widgets

import cc.xdan.tempo.model.Appearance
import org.junit.Assert.*
import org.junit.Test

class WidgetLocationTest {
    @Test fun existingOverridesKeepTheirBehaviour() {
        assertTrue(WidgetSettings().showsLocations(Appearance(showLocations = false)))
        assertFalse(WidgetSettings(showLocation = false).showsLocations(Appearance(showLocations = true)))
    }
    @Test fun followingLocationsTracksTimetableChanges() {
        val settings = WidgetSettings(showLocation = false, followLocations = true)
        assertTrue(settings.showsLocations(Appearance(showLocations = true)))
        assertFalse(settings.showsLocations(Appearance(showLocations = false)))
    }
}
