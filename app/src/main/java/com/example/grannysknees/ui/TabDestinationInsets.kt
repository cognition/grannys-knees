package com.example.grannysknees.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable

/**
 * Window insets for screens shown above the app bottom navigation.
 *
 * The outer [GrannysKneesApp] Scaffold already reserves space for the tab bar
 * (and the bar itself pads for system navigation). Including bottom
 * [safeDrawing] here again left a blank band above the menu.
 */
@Composable
internal fun tabDestinationWindowInsets(): WindowInsets =
    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
