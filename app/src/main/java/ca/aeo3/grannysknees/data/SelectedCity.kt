package ca.aeo3.grannysknees.data

/**
 * A city the user chose for the forecast.
 *
 * [label] is the short name shown on Today and the widget, for example
 * "London, Ontario".
 *
 * [placeSource] records whether the place came from GPS or city search so
 * pull-to-refresh can re-resolve location only when appropriate.
 */
internal data class SelectedCity(
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val admin1: String,
    val countryCode: String,
    val placeSource: PlaceSource = PlaceSource.Search,
) {
    /** True when this city is the one stored on [envelope]. */
    fun matches(envelope: ForecastCacheEnvelope): Boolean {
        val latitudeGap = kotlin.math.abs(latitude - envelope.latitude)
        val longitudeGap = kotlin.math.abs(longitude - envelope.longitude)
        return latitudeGap <= COORDINATE_TOLERANCE && longitudeGap <= COORDINATE_TOLERANCE
    }

    private companion object {
        const val COORDINATE_TOLERANCE: Double = 0.02
    }
}
