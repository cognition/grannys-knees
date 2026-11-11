package com.aetherealmind.grannysknees.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Selected city JSON round-trip, including place source defaults.
 */
class SelectedCityStoreTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `legacy city without placeSource defaults to Search`() {
        val file = temporaryFolder.newFile("legacy-city.json")
        file.writeText(
            """
            {"label":"Ottawa, Ontario","latitude":45.42,"longitude":-75.7,
            "timezone":"America/Toronto","admin1":"Ontario","countryCode":"CA"}
            """.trimIndent(),
            Charsets.UTF_8,
        )
        val store = SelectedCityStore(file)

        val city = store.read()

        assertEquals(PlaceSource.Search, city?.placeSource)
        assertEquals("Ottawa, Ontario", city?.label)
    }

    @Test
    fun `gps placeSource survives round trip`() {
        val store = SelectedCityStore(temporaryFolder.newFile("gps-city.json"))
        val original = SelectedCity(
            label = "London, Ontario",
            latitude = 42.98,
            longitude = -81.25,
            timezone = "America/Toronto",
            admin1 = "Ontario",
            countryCode = "CA",
            placeSource = PlaceSource.Gps,
        )

        store.write(original)

        assertEquals(PlaceSource.Gps, store.read()?.placeSource)
    }

    @Test
    fun `clear removes the city`() {
        val store = SelectedCityStore(temporaryFolder.newFile("clear-city.json"))
        store.write(
            SelectedCity(
                label = "Toronto, Ontario",
                latitude = 43.65,
                longitude = -79.38,
                timezone = "America/Toronto",
                admin1 = "Ontario",
                countryCode = "CA",
            ),
        )

        store.clear()

        assertNull(store.read())
    }
}
