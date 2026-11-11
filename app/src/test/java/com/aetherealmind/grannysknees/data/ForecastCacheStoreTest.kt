package com.aetherealmind.grannysknees.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant

class ForecastCacheStoreTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `round trips an envelope`() {
        val file = temporaryFolder.newFile("cache.json")
        val store = ForecastCacheStore(file)
        val envelope = ForecastCacheEnvelope(
            fetchedAt = Instant.parse("2026-11-10T00:05:00Z"),
            locationLabel = "Toronto, ON",
            latitude = 43.65,
            longitude = -79.38,
            timezone = "America/Toronto",
            rawBody = """{"ok":true}""",
        )

        store.write(envelope)
        val read = store.read()

        assertEquals(envelope, read)
    }

    @Test
    fun `corrupt file yields null`() {
        val file = temporaryFolder.newFile("bad.json")
        file.writeText("{not-json", Charsets.UTF_8)

        assertNull(ForecastCacheStore(file).read())
    }
}
