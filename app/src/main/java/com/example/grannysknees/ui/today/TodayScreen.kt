package com.example.grannysknees.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.grannysknees.R
import com.example.grannysknees.ui.theme.GrannysKneesTheme

/**
 * Today destination entry point.
 *
 * Reads a [TodayUiState] from an injected [provider] and renders it. The
 * default [FakeTodayStateProvider] supplies placeholder data so the screen
 * works end-to-end before weather and scoring are connected (T06).
 */
@Composable
internal fun TodayRoute(
    modifier: Modifier = Modifier,
    provider: TodayStateProvider = FakeTodayStateProvider(),
) {
    TodayScreen(state = provider.current(), modifier = modifier)
}

/** Stateless Today screen: shows the index, freshness and the disclaimer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodayScreen(
    state: TodayUiState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.today_title)) })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            LocationHeader(location = state.location)
            when (state) {
                is TodayUiState.Available -> {
                    IndexCard(state = state)
                    LastUpdated(label = state.lastUpdatedLabel, isStale = state.isStale)
                }

                is TodayUiState.Unavailable -> OfflineNotice(state = state)
            }
            ExperimentalDisclaimer()
        }
    }
}

@Composable
private fun LocationHeader(location: String) {
    val locationDescription =
        stringResource(R.string.today_location_content_description, location)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(R.string.today_location_eyebrow),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = location,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics {
                contentDescription = locationDescription
            },
        )
    }
}

@Composable
private fun IndexCard(state: TodayUiState.Available) {
    val bandLabel = bandLabel(state.band)
    val scoreDescription = stringResource(
        R.string.today_index_content_description,
        state.score,
        TodayScoreBand.MAX_SCORE,
        bandLabel,
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.today_index_heading),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = scoreDescription
                },
            ) {
                Text(
                    text = state.score.toString(),
                    style = MaterialTheme.typography.displayLarge,
                )
                Text(
                    text = stringResource(
                        R.string.today_index_out_of,
                        TodayScoreBand.MAX_SCORE,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
                )
            }
            BandPill(label = bandLabel)
            Text(
                text = bandCaption(state.band),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BandPill(label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun LastUpdated(label: String, isStale: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.today_last_updated, label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isStale) {
            StaleAffordance()
        }
    }
}

@Composable
private fun StaleAffordance() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.today_stale_badge),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(R.string.today_stale_detail),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun OfflineNotice(state: TodayUiState.Unavailable) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.today_offline_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.today_offline_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.today_last_updated, state.lastUpdatedLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExperimentalDisclaimer() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.today_disclaimer_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.today_disclaimer_body),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun bandLabel(band: TodayScoreBand): String = stringResource(
    when (band) {
        TodayScoreBand.Calm -> R.string.today_band_calm
        TodayScoreBand.Mild -> R.string.today_band_mild
        TodayScoreBand.Noticeable -> R.string.today_band_noticeable
        TodayScoreBand.Rough -> R.string.today_band_rough
        TodayScoreBand.Stormy -> R.string.today_band_stormy
    },
)

@Composable
private fun bandCaption(band: TodayScoreBand): String = stringResource(
    when (band) {
        TodayScoreBand.Calm -> R.string.today_caption_calm
        TodayScoreBand.Mild -> R.string.today_caption_mild
        TodayScoreBand.Noticeable -> R.string.today_caption_noticeable
        TodayScoreBand.Rough -> R.string.today_caption_rough
        TodayScoreBand.Stormy -> R.string.today_caption_stormy
    },
)

@Preview(name = "Fresh", showBackground = true)
@Composable
private fun TodayScreenFreshPreview() {
    GrannysKneesTheme {
        TodayScreen(state = SampleTodayStates.fresh)
    }
}

@Preview(name = "Stale", showBackground = true)
@Composable
private fun TodayScreenStalePreview() {
    GrannysKneesTheme {
        TodayScreen(state = SampleTodayStates.stale)
    }
}

@Preview(name = "Offline", showBackground = true)
@Composable
private fun TodayScreenOfflinePreview() {
    GrannysKneesTheme {
        TodayScreen(state = SampleTodayStates.offline)
    }
}
