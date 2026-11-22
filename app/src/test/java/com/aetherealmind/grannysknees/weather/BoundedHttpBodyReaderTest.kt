package com.aetherealmind.grannysknees.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * Bounded body reads for [UrlHttpTransport]: oversized payloads must fail
 * closed rather than buffering unbounded JSON into memory.
 */
class BoundedHttpBodyReaderTest {

    @Test
    fun `reads a body within the byte limit`() {
        val body = """{"ok":true}"""
        val text = BoundedHttpBodyReader.readUtf8(
            ByteArrayInputStream(body.toByteArray(Charsets.UTF_8)),
            maxBytes = 64,
        )
        assertEquals(body, text)
    }

    @Test
    fun `rejects a body that exceeds the byte limit`() {
        val payload = "x".repeat(32)
        val error = assertThrows(IOException::class.java) {
            BoundedHttpBodyReader.readUtf8(
                ByteArrayInputStream(payload.toByteArray(Charsets.UTF_8)),
                maxBytes = 16,
            )
        }
        assertTrue(error.message!!.contains("exceeded"))
    }

    @Test
    fun `default transport cap is two mebibytes`() {
        assertEquals(2 * 1024 * 1024, UrlHttpTransport.DEFAULT_MAX_RESPONSE_BYTES)
    }
}
