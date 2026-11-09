package com.example.grannysknees.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.example.grannysknees.ui.tabDestinationWindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.grannysknees.R
import com.example.grannysknees.data.ForecastRepository
import com.example.grannysknees.data.PlaceSource
import com.example.grannysknees.data.SelectedCity
import com.example.grannysknees.data.SelectedCityStore
import com.example.grannysknees.location.OptionalGpsPlaceSource
import com.example.grannysknees.sayings.GrannySaysCatalog
import com.example.grannysknees.sayings.GrannySaysPicker
import com.example.grannysknees.sayings.GrannySeason
import com.example.grannysknees.ui.place.CitySearchScreen
import com.example.grannysknees.weather.OpenMeteoGeocodingClient
import com.example.grannysknees.ui.outlook.OutlookSection
import com.example.grannysknees.ui.outlook.OutlookUiState
import com.example.grannysknees.ui.outlook.SampleOutlookStates
import com.example.grannysknees.ui.theme.BrandHeading
import com.example.grannysknees.ui.theme.GrannysKneesTheme
import com.example.grannysknees.ui.theme.personalIndexColour
import com.example.grannysknees.widget.GrannysKneesWidget
import androidx.glance.appwidget.updateAll
import java.math.RoundingMode
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Today destination entry point.
 *
 * Fetches weather at most once per local day on open; tab changes and resume
 * re-present the disk cache. Pull-to-refresh forces a network pull (and GPS
 * re-resolve when the saved place came from the phone).
 */
@Composable
internal fun TodayRoute(
    modifier: Modifier = Modifier,
    repository: ForecastRepository? = null,
    cityStore: SelectedCityStore? = null,
    sensitivityEpoch: Int = 0,
    grannySpeaksFreely: Boolean = false,
    geocoder: OpenMeteoGeocodingClient = OpenMeteoGeocodingClient(),
    provider: TodayStateProvider = FakeTodayStateProvider(),
) {
    if (repository == null) {
        TodayScreen(
            state = provider.current(),
            modifier = modifier,
            grannySpeaksFreely = grannySpeaksFreely,
        )
        return
    }

    var chosen by remember { mutableStateOf(cityStore?.read()) }
    var forceNetworkOnNextLoad by remember { mutableStateOf(false) }
    var sessionLoadKey by remember { mutableStateOf(0) }
    if (cityStore != null && chosen == null) {
        CitySearchScreen(
            geocoder = geocoder,
            onCityChosen = { city: SelectedCity ->
                cityStore.write(city)
                forceNetworkOnNextLoad = true
                chosen = city
                sessionLoadKey += 1
            },
            modifier = modifier,
        )
        return
    }

    var state by remember {
        mutableStateOf<TodayUiState>(
            TodayUiState.Loading(location = chosen?.label.orEmpty()),
        )
    }
    var outlook by remember { mutableStateOf<OutlookUiState>(OutlookUiState.Hidden) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val gps = remember { OptionalGpsPlaceSource(context) }
    var initialLoadDone by remember { mutableStateOf(false) }

    suspend fun presentFromDisk() {
        state = withContext(Dispatchers.IO) { repository.presentFromCache() }
        outlook = withContext(Dispatchers.IO) { repository.presentOutlookFromCache() }
        withContext(Dispatchers.IO) { GrannysKneesWidget().updateAll(context) }
    }

    suspend fun loadForecast(forceNetwork: Boolean) {
        if (forceNetwork &&
            chosen?.placeSource == PlaceSource.Gps &&
            cityStore != null &&
            gps.hasPermission()
        ) {
            val updated = withContext(Dispatchers.IO) { gps.currentCity() }
            if (updated != null) {
                withContext(Dispatchers.IO) { cityStore.write(updated) }
                chosen = updated
            }
        }
        state = withContext(Dispatchers.IO) {
            repository.loadToday(forceNetwork = forceNetwork)
        }
        outlook = withContext(Dispatchers.IO) { repository.presentOutlookFromCache() }
        withContext(Dispatchers.IO) { GrannysKneesWidget().updateAll(context) }
    }

    LaunchedEffect(repository, sessionLoadKey) {
        state = TodayUiState.Loading(location = chosen?.label.orEmpty())
        outlook = OutlookUiState.Hidden
        val forceNetwork = forceNetworkOnNextLoad
        forceNetworkOnNextLoad = false
        loadForecast(forceNetwork = forceNetwork)
        initialLoadDone = true
        while (true) {
            val wait = withContext(Dispatchers.IO) {
                repository.delayUntilNextPresentationChange()
            }
            delay(wait.toMillis().coerceAtLeast(1L))
            presentFromDisk()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (!initialLoadDone) return@LifecycleEventEffect
        scope.launch { presentFromDisk() }
    }

    LaunchedEffect(sensitivityEpoch) {
        if (!initialLoadDone || sensitivityEpoch == 0) return@LaunchedEffect
        presentFromDisk()
    }

    TodayScreen(
        state = state,
        outlook = outlook,
        modifier = modifier,
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                try {
                    loadForecast(forceNetwork = true)
                } finally {
                    isRefreshing = false
                }
            }
        },
        onChangeCity = if (cityStore == null) {
            null
        } else {
            {
                cityStore.clear()
                chosen = null
            }
        },
        grannySpeaksFreely = grannySpeaksFreely,
    )
}

/** Stateless Today screen: shows the index, freshness, outlook and the disclaimer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodayScreen(
    state: TodayUiState,
    modifier: Modifier = Modifier,
    outlook: OutlookUiState = OutlookUiState.Hidden,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    onChangeCity: (() -> Unit)? = null,
    grannySpeaksFreely: Boolean = false,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = tabDestinationWindowInsets(),
        topBar = {
            TopAppBar(
                title = {
                    BrandHeading(title = stringResource(R.string.today_title))
                },
                actions = {
                    if (onRefresh != null) {
                        val refreshDescription =
                            stringResource(R.string.today_refresh_content_description)
                        TextButton(
                            onClick = onRefresh,
                            enabled = !isRefreshing,
                            modifier = Modifier.semantics {
                                contentDescription = refreshDescription
                            },
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(stringResource(R.string.today_refresh))
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val body: @Composable () -> Unit = {
            // No vertical scroll: header stays compact and outlook fills leftover height
            // so Today fits phones of different sizes without scrolling.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when (state) {
                    is TodayUiState.Loading -> {
                        LocationHeader(
                            location = state.location,
                            onChangeCity = onChangeCity,
                        )
                        LoadingNotice(
                            modifier = Modifier.weight(1f, fill = true),
                        )
                    }

                    is TodayUiState.Available -> {
                        // Tight location → score; saying keeps its own breathing room.
                        Column(
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            LocationHeader(
                                location = state.location,
                                onChangeCity = onChangeCity,
                            )
                            IndexCard(
                                state = state,
                                grannySpeaksFreely = grannySpeaksFreely,
                            )
                        }
                        LastUpdated(
                            label = state.lastUpdatedLabel,
                            isStale = state.isStale,
                        )
                    }

                    is TodayUiState.Unavailable -> {
                        LocationHeader(
                            location = state.location,
                            onChangeCity = onChangeCity,
                        )
                        OfflineNotice(
                            state = state,
                            onRefresh = onRefresh,
                            isRefreshing = isRefreshing,
                            modifier = Modifier.weight(1f, fill = true),
                        )
                    }
                }
                if (outlook is OutlookUiState.Available) {
                    OutlookSection(
                        state = outlook,
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .fillMaxWidth(),
                    )
                }
            }
        }
        if (onRefresh == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                body()
            }
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                body()
            }
        }
    }
}

@Composable
private fun LocationHeader(location: String, onChangeCity: (() -> Unit)?) {
    val locationDescription =
        stringResource(R.string.today_location_content_description, location)
    val changeDescription = stringResource(R.string.place_change_content_description)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = location,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .semantics {
                    contentDescription = locationDescription
                },
        )
        if (onChangeCity != null) {
            TextButton(
                onClick = onChangeCity,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 1.dp)
                    .heightIn(max = 28.dp)
                    .semantics { contentDescription = changeDescription },
            ) {
                Text(
                    text = stringResource(R.string.place_change),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun IndexCard(
    state: TodayUiState.Available,
    grannySpeaksFreely: Boolean = false,
) {
    val bandLabel = bandLabel(state.band)
    val scoreLabel = state.scoreLabel
    val personalLabel = state.personalScoreLabel
    val scoreDescription = if (personalLabel == null) {
        stringResource(
            R.string.today_index_content_description,
            scoreLabel,
            bandLabel,
        )
    } else {
        stringResource(
            R.string.today_index_personal_content_description,
            scoreLabel,
            personalLabel,
            bandLabel,
        )
    }
    val context = LocalContext.current
    val picker = remember {
        loadGrannySaysPicker(context.assets)
    }
    val season = GrannySeason.current(LocalDate.now().monthValue)
    var sayingText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(
        state.score,
        state.personalScore,
        grannySpeaksFreely,
        state.forecastHasSnow,
        picker,
        season,
    ) {
        sayingText = picker?.showForScore(
            score = state.score.setScale(0, RoundingMode.HALF_UP).intValueExact(),
            personalScore = state.personalScore?.setScale(0, RoundingMode.HALF_UP)?.intValueExact(),
            allowFreeSpeech = grannySpeaksFreely,
            season = season,
            snowInForecast = state.forecastHasSnow,
        )?.text
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = scoreDescription
            },
        ) {
            Text(
                text = scoreLabel,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
            )
            if (personalLabel != null) {
                Text(
                    text = "($personalLabel)",
                    style = MaterialTheme.typography.titleLarge,
                    color = personalIndexColour(),
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Spacer(modifier = Modifier.weight(1f, fill = true))
            BandPill(band = state.band, label = bandLabel)
        }
        Text(
            text = bandCaption(state.band),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // Always reserve two saying lines so Another saying never shifts the layout.
        if (picker != null) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 6.dp),
            ) {
                CircularRefreshButton(
                    onClick = {
                        val next = picker.refresh()?.text
                        if (next != null) {
                            sayingText = next
                        }
                    },
                    contentDescription = stringResource(R.string.today_another_saying),
                    buttonSize = 28.dp,
                    iconSize = 16.dp,
                    enabled = sayingText != null,
                )
                Text(
                    text = sayingText.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = true,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = true),
                )
            }
        }
    }
}

/** Round refresh control shared by the top bar and Another saying. */
@Composable
private fun CircularRefreshButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    buttonSize: Dp = 32.dp,
    iconSize: Dp = 18.dp,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled && !busy,
        modifier = modifier.size(buttonSize),
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(iconSize),
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_refresh),
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

/** Loads the packaged Granny Says list, or null when the asset is missing. */
private fun loadGrannySaysPicker(assets: android.content.res.AssetManager): GrannySaysPicker? {
    return try {
        assets.open("granny-says.json").use { stream ->
            GrannySaysPicker(GrannySaysCatalog.fromStream(stream))
        }
    } catch (_: IOException) {
        null
    }
}

@Composable
private fun BandPill(band: TodayScoreBand, label: String) {
    val (container, onContainer) = when (band) {
        TodayScoreBand.Rough, TodayScoreBand.Stormy ->
            MaterialTheme.colorScheme.secondaryContainer to
                MaterialTheme.colorScheme.onSecondaryContainer
        TodayScoreBand.Noticeable ->
            MaterialTheme.colorScheme.tertiaryContainer to
                MaterialTheme.colorScheme.onTertiaryContainer
        TodayScoreBand.Calm, TodayScoreBand.Mild ->
            MaterialTheme.colorScheme.primaryContainer to
                MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = onContainer,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun LastUpdated(label: String, isStale: Boolean) {
    val text = if (isStale) {
        stringResource(R.string.today_stale_line, label)
    } else {
        stringResource(R.string.today_updated, label)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LoadingNotice(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.today_loading_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.today_loading_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun OfflineNotice(
    state: TodayUiState.Unavailable,
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
) {
    val titleRes = when (state.reason) {
        UnavailableReason.EMPTY -> R.string.today_offline_title
        UnavailableReason.EXPIRED -> R.string.today_expired_title
        UnavailableReason.INVALID_HOUR -> R.string.today_invalid_hour_title
    }
    val bodyRes = when (state.reason) {
        UnavailableReason.EMPTY -> R.string.today_offline_body
        UnavailableReason.EXPIRED -> R.string.today_expired_body
        UnavailableReason.INVALID_HOUR -> R.string.today_invalid_hour_body
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.today_last_updated, state.lastUpdatedLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onRefresh != null &&
                (state.reason == UnavailableReason.EXPIRED ||
                    state.reason == UnavailableReason.EMPTY)
            ) {
                Button(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.today_refresh))
                }
            }
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
        TodayScreen(state = SampleTodayStates.fresh, outlook = SampleOutlookStates.withGap)
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
