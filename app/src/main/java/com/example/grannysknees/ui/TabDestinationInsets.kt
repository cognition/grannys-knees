package com.example.grannysknees.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Window insets for screens shown beside or above the app tab chrome.
 *
 * Portrait: the outer Scaffold bottom bar owns system-nav / bottom insets.
 * Landscape: the left side rail owns start insets for itself (plus top/bottom
 * so rail icons clear the system bars). Destination content still needs top /
 * end / bottom safe drawing; repeating start here left a blank band next to
 * the menu.
 */
@Composable
internal fun tabDestinationWindowInsets(): WindowInsets {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    return if (isLandscape) {
        WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.End + WindowInsetsSides.Bottom,
        )
    } else {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
    }
}
