package com.aetherealmind.grannysknees.journal

/**
 * Flat CSV for the current journal (one row per measurement).
 *
 * Tombstones and corrected rows are omitted by the caller via [JournalRepository.current].
 */
internal object JournalCsvExport {
    const val FILE_NAME: String = "grannys-knees-history.csv"
    const val MIME_TYPE: String = "text/csv"

    private val HEADER: String =
        "observationId,observedAt,bodyPart,side,symptom,rating,reportKind"

    /** Builds a CSV document for [observations], newest-first or caller order. */
    fun format(observations: List<JournalObservation>): String {
        val lines = ArrayList<String>(1 + observations.sumOf { it.measurements.size })
        lines += HEADER
        observations.forEach { observation ->
            observation.measurements.forEach { measurement ->
                lines += listOf(
                    escape(observation.id),
                    escape(observation.observedAt.toString()),
                    escape(measurement.bodyPart),
                    escape(measurement.side.orEmpty()),
                    escape(measurement.symptom),
                    measurement.rating.toString(),
                    escape(measurement.reportKind),
                ).joinToString(",")
            }
        }
        return lines.joinToString(separator = "\n", postfix = "\n")
    }

    private fun escape(value: String): String {
        if (value.none { ch -> ch == ',' || ch == '"' || ch == '\n' || ch == '\r' }) {
            return value
        }
        return buildString {
            append('"')
            value.forEach { ch ->
                if (ch == '"') append('"')
                append(ch)
            }
            append('"')
        }
    }
}
