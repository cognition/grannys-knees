package com.aetherealmind.grannysknees.data

/**
 * How the user chose the forecast place.
 *
 * Pull-to-refresh re-resolves GPS only for [Gps]; [Search] keeps the saved
 * coordinates and refreshes weather only.
 */
internal enum class PlaceSource {
    Gps,
    Search,
}
