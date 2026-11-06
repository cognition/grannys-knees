package com.example.grannysknees.ui

internal enum class AppDestination(
    val route: String,
    val title: String,
) {
    Today(
        route = "today",
        title = "Granny's Knees",
    ),
    Home(
        route = "home",
        title = "Granny's Knees",
    ),
    Journal(
        route = "journal",
        title = "How you feel",
    ),
    History(
        route = "history",
        title = "Your reports",
    ),
    Privacy(
        route = "privacy",
        title = "Your privacy",
    ),
    About(
        route = "about",
        title = "About the index",
    ),
}
