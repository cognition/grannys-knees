package com.example.grannysknees.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.grannysknees.R

/**
 * The approved mark. Dark and light artwork follow the phone theme.
 */
@Composable
internal fun BrandLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = stringResource(R.string.logo_content_description),
) {
    val artwork = if (isSystemInDarkTheme()) R.drawable.ic_logo_dark else R.drawable.ic_logo_light
    Image(
        painter = painterResource(artwork),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}
