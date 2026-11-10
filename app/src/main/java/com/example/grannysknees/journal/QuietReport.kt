package com.example.grannysknees.journal

import com.example.grannysknees.R
import java.util.Locale

/**
 * One-line title for a saved measurement, such as "Left knee ache".
 */
internal fun quietReportTitle(side: String?, bodyPart: String, symptom: String): String {
    val sideLabel = side?.replaceFirstChar { character ->
        if (character.isLowerCase()) character.titlecase(Locale.CANADA) else character.toString()
    }
    val words = listOfNotNull(sideLabel, bodyPart, symptom).joinToString(separator = " ")
    return words.replaceFirstChar { character ->
        if (character.isLowerCase()) character.titlecase(Locale.CANADA) else character.toString()
    }
}

/**
 * Granny's aside for the place being reported.
 */
@Suppress("UNUSED_PARAMETER")
internal fun grannyAside(part: String, side: String?): Int {
    return when (part) {
        "head" -> R.string.journal_granny_head
        "neck" -> R.string.journal_granny_neck
        "shoulder" -> R.string.journal_granny_shoulder
        "elbow" -> R.string.journal_granny_elbow
        "wrist" -> R.string.journal_granny_wrist
        "hand" -> R.string.journal_granny_hand
        "back" -> R.string.journal_granny_back
        "hip" -> R.string.journal_granny_hip
        "knee" -> R.string.journal_granny_knee
        "ankle" -> R.string.journal_granny_ankle
        "foot" -> R.string.journal_granny_foot
        else -> R.string.journal_granny_other
    }
}

/**
 * History title line with rating beside the label, then the date.
 *
 * Example: "Left wrist stiffness, 8 - Tue 10 Nov · 14:30".
 */
internal fun quietHistoryHeadline(
    side: String?,
    bodyPart: String,
    symptom: String,
    rating: Int,
    whenLabel: String,
): String {
    val label = quietReportTitle(side, bodyPart, symptom)
    return "$label, $rating - $whenLabel"
}

/**
 * Secondary history line: whether the report changed or stayed the same.
 */
internal fun quietReportDetail(reportKind: String): String {
    return if (reportKind == JournalCommitter.CONFIRMED_UNCHANGED) {
        "unchanged"
    } else {
        "changed"
    }
}
