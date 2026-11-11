package com.aetherealmind.grannysknees.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDestinationTest {
    @Test
    fun `routes are non-empty and unique`() {
        val routes = AppDestination.entries.map(AppDestination::route)

        assertTrue(routes.all(String::isNotBlank))
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun `home destination carries product title`() {
        assertEquals("home", AppDestination.Home.route)
        assertEquals("Granny's Knees", AppDestination.Home.title)
    }

    @Test
    fun `today destination is registered as the primary route`() {
        assertEquals("today", AppDestination.Today.route)
        assertEquals("Granny's Knees", AppDestination.Today.title)
    }
}
