package com.aetherealmind.grannysknees.data

/**
 * Fixed MVP forecast location until a picker exists (T06 / R0).
 *
 * Matches sample UI copy and DST fixtures. Not a product commitment to serve
 * only Toronto — labelled temporary in the UI eyebrow copy.
 */
internal object DefaultLocation {
    const val LABEL: String = "Toronto, ON"
    const val LATITUDE: Double = 43.65
    const val LONGITUDE: Double = -79.38
    const val TIMEZONE: String = "America/Toronto"
}
