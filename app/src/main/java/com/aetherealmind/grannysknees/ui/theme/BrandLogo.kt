package com.aetherealmind.grannysknees.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aetherealmind.grannysknees.R

/** Default mark size in app bar titles. */
private val BrandHeadingLogoSize = 40.dp

/**
 * The approved mark. Dark and light artwork follow the active app theme.
 */
@Composable
internal fun BrandLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = stringResource(R.string.logo_content_description),
) {
    val artwork = if (LocalUseDarkTheme.current) {
        R.drawable.ic_logo_dark
    } else {
        R.drawable.ic_logo_light
    }
    Image(
        painter = painterResource(artwork),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}

/**
 * App-bar heading used across the product: brand mark plus screen title.
 */
@Composable
internal fun BrandHeading(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BrandLogo(
            modifier = Modifier.size(BrandHeadingLogoSize),
            contentDescription = null,
        )
        Text(text = title)
    }
}

