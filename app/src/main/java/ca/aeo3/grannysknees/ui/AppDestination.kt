package ca.aeo3.grannysknees.ui

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
        title = "History",
    ),
    Privacy(
        route = "privacy",
        title = "Privacy policy",
    ),
    Settings(
        route = "settings",
        title = "Settings",
    ),
    About(
        route = "about",
        title = "About the index",
    ),
}
