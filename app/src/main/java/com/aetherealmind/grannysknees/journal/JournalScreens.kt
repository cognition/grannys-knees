package com.aetherealmind.grannysknees.journal

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.aetherealmind.grannysknees.ui.tabDestinationWindowInsets
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.aetherealmind.grannysknees.R
import com.aetherealmind.grannysknees.data.FeelCalibrationStore
import com.aetherealmind.grannysknees.data.ForecastRepository
import com.aetherealmind.grannysknees.ui.theme.BrandHeading
import com.aetherealmind.grannysknees.ui.theme.GrannyCorner
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SYMPTOMS: List<String> = listOf("ache", "stiffness")
private const val WEEK_FRAME_MILLIS: Long = 1200L
private val NOTICE_HEIGHT = 18.dp
private val ACTION_PADDING = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
private val COMPACT_BUTTON_HEIGHT = 36.dp
private val HISTORY_THUMB_HEIGHT = 72.dp
private val HISTORY_CARD_CORNER = 8.dp
private val HISTORY_ROW_GAP = 4.dp

/** Partial symptom entry. Chosen places add together, and only those are written. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun JournalScreen(
    repository: ForecastRepository? = null,
    feelCalibrationStore: FeelCalibrationStore? = null,
    feelCalibrationEnabled: Boolean = true,
    onFeelCalibrationEnabledChange: (Boolean) -> Unit = {},
    onFeelSaved: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val forecastRepository = repository
    val journalRepository = remember {
        JournalRepository(
            Room.databaseBuilder(
                context.applicationContext,
                JournalDatabase::class.java,
                "journal.db",
            ).build().journalDao(),
        )
    }
    val zone = ZoneId.systemDefault()
    val store = remember { ReportDummyStore(File(context.filesDir, "report-dummy.json")) }
    val opened = remember { store.read(Instant.now(), zone) }
    var chosen by remember { mutableStateOf(opened.places) }
    var focus by remember { mutableIntStateOf(opened.focus) }
    var notice by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }
    var framePlaces by remember { mutableStateOf<List<ChosenPlace>?>(null) }
    var frameLabel by remember { mutableStateOf<String?>(null) }
    val saved = stringResource(R.string.journal_saved)
    val nothing = stringResource(R.string.journal_nothing_saved)
    val noYesterday = stringResource(R.string.journal_no_yesterday)
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEE d MMM", Locale.CANADA) }
    var todayLabel by remember {
        mutableStateOf(dayFormatter.format(ReportDay.reportingDay(Instant.now(), zone)))
    }
    val scope = rememberCoroutineScope()
    LaunchedEffect(chosen, focus) {
        if (chosen.isEmpty()) {
            return@LaunchedEffect
        }
        val draft = ReportDummy(
            places = chosen,
            focus = focus.coerceIn(0, chosen.lastIndex),
            savedAt = Instant.now(),
        )
        withContext(Dispatchers.IO) { store.write(draft) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            val now = Instant.now()
            val boundary = ReportDay.nextBoundary(now, zone)
            delay(Duration.between(now, boundary).toMillis().coerceAtLeast(1L))
            val cleared = store.fresh(Instant.now())
            withContext(Dispatchers.IO) { store.write(cleared) }
            chosen = cleared.places
            focus = 0
            notice = null
            playing = false
            todayLabel = dayFormatter.format(ReportDay.reportingDay(Instant.now(), zone))
        }
    }
    LaunchedEffect(playing) {
        if (!playing) {
            framePlaces = null
            frameLabel = null
            return@LaunchedEffect
        }
        val frames = withContext(Dispatchers.IO) {
            weekFrames(journalRepository.all(), Instant.now(), zone)
        }
        frames.forEach { frame ->
            framePlaces = frame.places
            frameLabel = frame.label
            delay(WEEK_FRAME_MILLIS)
        }
        playing = false
    }
    val displayed = framePlaces ?: chosen
    val editing = framePlaces == null
    val displayedFocus = if (editing) focus else 0
    val place = displayed.getOrNull(displayedFocus)
    fun addOrFocus(part: String, side: String?) {
        if (!editing || chosen.isEmpty()) {
            return
        }
        val existing = chosen.indexOfFirst { it.part == part && it.side == side }
        if (existing >= 0) {
            focus = existing
        } else {
            val current = chosen.getOrNull(focus.coerceIn(0, chosen.lastIndex)) ?: return
            chosen = chosen + ChosenPlace(
                part,
                side,
                current.symptom,
                ratingForSymptom(current.symptom, current.rating),
            )
            focus = chosen.lastIndex
        }
    }
    fun replaceFocus(next: ChosenPlace) {
        if (!editing) {
            return
        }
        val adjusted = next.copy(
            rating = ratingForSymptom(next.symptom, next.rating),
        )
        chosen = chosen.mapIndexed { index, item -> if (index == focus) adjusted else item }
    }

    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val reportTitle = if (place != null) {
        quietReportTitle(place.side, place.part, place.symptom)
    } else {
        stringResource(R.string.journal_day_empty)
    }
    val accent = MaterialTheme.colorScheme.primary
    val quietLine = MaterialTheme.colorScheme.outlineVariant
    val ratingDescription = if (place != null) {
        stringResource(R.string.journal_rating, place.rating)
    } else {
        stringResource(R.string.journal_rating_empty)
    }

    @Composable
    fun JournalFigure(panelModifier: Modifier) {
        Box(
            modifier = panelModifier,
            contentAlignment = Alignment.Center,
        ) {
            BodyFigure(
                marks = displayed.map { item ->
                    FigureMark(
                        part = item.part,
                        side = item.side,
                        rating = item.rating,
                        ache = item.symptom == "ache",
                    )
                },
                onSelect = { selectedPart, selectedSide ->
                    addOrFocus(selectedPart, selectedSide)
                },
                enabled = editing,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(
                        BodyMap.WIDTH / BodyMap.HEIGHT,
                        matchHeightConstraintsFirst = true,
                    ),
            )
            TextButton(
                onClick = {
                    if (!editing) {
                        return@TextButton
                    }
                    val cleared = store.fresh(Instant.now())
                    scope.launch {
                        withContext(Dispatchers.IO) { store.write(cleared) }
                    }
                    chosen = cleared.places
                    focus = 0
                    playing = false
                    framePlaces = null
                    frameLabel = null
                    notice = context.getString(R.string.journal_figure_clear_done)
                },
                enabled = editing,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.journal_figure_clear),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }

    @Composable
    fun JournalControls(panelModifier: Modifier, showReportHeading: Boolean) {
        Column(
            modifier = panelModifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (showReportHeading) {
                Text(
                    reportTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    place?.let { current ->
                        stringResource(grannyAside(current.part, current.side))
                    } ?: "\u00a0",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SYMPTOMS.forEach { option ->
                    InkChoice(
                        label = option.replaceFirstChar { it.titlecase() },
                        selected = place?.symptom == option,
                        enabled = editing && place != null,
                        onClick = {
                            val current = place ?: return@InkChoice
                            replaceFocus(
                                current.copy(
                                    symptom = option,
                                    rating = ratingForSymptom(option, current.rating),
                                ),
                            )
                        },
                    )
                }
            }
            if (displayed.size > 1) {
                Text(
                    displayed.joinToString(separator = ", ") { item ->
                        quietReportTitle(item.side, item.part, item.symptom)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                place?.let { current ->
                    stringResource(R.string.journal_rating, current.rating)
                } ?: stringResource(R.string.journal_rating_empty),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Slider(
                value = place?.rating?.toFloat() ?: 0f,
                onValueChange = { value ->
                    if (!editing || place == null) {
                        return@Slider
                    }
                    chosen = chosen.mapIndexed { index, item ->
                        if (index == focus) {
                            item.copy(rating = ratingForSymptom(item.symptom, value.toInt()))
                        } else {
                            item
                        }
                    }
                },
                enabled = editing && place != null,
                valueRange = if (place?.symptom == "ache") 1f..10f else 0f..10f,
                steps = if (place?.symptom == "ache") 8 else 9,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = quietLine,
                    activeTickColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveTickColor = accent.copy(alpha = 0.35f),
                ),
                modifier = Modifier.semantics {
                    contentDescription = ratingDescription
                },
            )
            Button(
                onClick = {
                    scope.launch {
                        val edits = chosen.map { item ->
                            SeriesEdit(
                                item.part,
                                item.side,
                                item.symptom,
                                ratingForSymptom(item.symptom, item.rating),
                            )
                        }
                        val now = Instant.now()
                        val wrote = withContext(Dispatchers.IO) {
                            val observation = journalRepository.save(
                                committedAt = now,
                                edits = edits,
                                confirmations = emptyList(),
                            )
                            if (observation != null) {
                                recordFeelCalibration(
                                    forecastRepository = forecastRepository,
                                    feelCalibrationStore = feelCalibrationStore,
                                    observationId = observation.id,
                                    observedAt = now,
                                    ratings = edits.map { it.rating },
                                )
                            }
                            observation
                        }
                        notice = if (wrote == null) nothing else saved
                        if (wrote != null) {
                            onFeelSaved()
                        }
                    }
                },
                enabled = editing,
                shape = GrannyCorner,
                contentPadding = ACTION_PADDING,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(COMPACT_BUTTON_HEIGHT),
            ) {
                Text(
                    text = stringResource(R.string.journal_save_change),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                QuietAction(
                    label = stringResource(
                        if (playing) R.string.journal_stop_week else R.string.journal_play_week,
                    ),
                    onClick = { playing = !playing },
                )
                QuietAction(
                    label = stringResource(R.string.journal_same_as_yesterday),
                    enabled = editing,
                    onClick = {
                        if (!editing) {
                            return@QuietAction
                        }
                        scope.launch {
                            val places = withContext(Dispatchers.IO) {
                                placesFromYesterday(journalRepository.all(), Instant.now(), zone)
                            }
                            if (places.isEmpty()) {
                                notice = noYesterday
                            } else {
                                chosen = places.map { item ->
                                    item.copy(
                                        rating = ratingForSymptom(item.symptom, item.rating),
                                    )
                                }
                                focus = chosen.lastIndex
                                notice = null
                            }
                        }
                    },
                )
                QuietAction(
                    label = stringResource(R.string.journal_unchanged),
                    enabled = editing,
                    onClick = {
                        if (!editing) {
                            return@QuietAction
                        }
                        scope.launch {
                            val keys = chosen.map { item ->
                                SeriesKey(item.part, item.side, item.symptom)
                            }
                            val now = Instant.now()
                            val wrote = withContext(Dispatchers.IO) {
                                val observation = journalRepository.save(
                                    committedAt = now,
                                    edits = emptyList(),
                                    confirmations = keys,
                                )
                                if (observation != null) {
                                    recordFeelCalibration(
                                        forecastRepository = forecastRepository,
                                        feelCalibrationStore = feelCalibrationStore,
                                        observationId = observation.id,
                                        observedAt = now,
                                        ratings = observation.measurements.map { it.rating },
                                    )
                                }
                                observation
                            }
                            notice = if (wrote == null) nothing else saved
                            if (wrote != null) {
                                onFeelSaved()
                            }
                        }
                    },
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(NOTICE_HEIGHT),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (notice != null) {
                    Text(
                        notice.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.journal_feel_tune_compact),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = feelCalibrationEnabled,
                    onCheckedChange = onFeelCalibrationEnabledChange,
                    modifier = Modifier.scale(0.55f),
                )
            }
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = tabDestinationWindowInsets(),
        topBar = {
            if (!isLandscape) {
                TopAppBar(
                    title = {
                        BrandHeading(title = stringResource(R.string.journal_open))
                    },
                )
            }
        },
    ) { innerPadding ->
        if (isLandscape) {
            // Option A: figure left, controls right, compact one-line chrome.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.journal_open),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        text = stringResource(R.string.journal_intro),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = true),
                    )
                    Text(
                        frameLabel ?: todayLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    JournalFigure(
                        panelModifier = Modifier
                            .weight(1.05f)
                            .fillMaxHeight(),
                    )
                    JournalControls(
                        panelModifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        showReportHeading = true,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            stringResource(R.string.journal_intro),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(R.string.journal_feel_no_backdate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            reportTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            place?.let { current ->
                                stringResource(grannyAside(current.part, current.side))
                            } ?: "\u00a0",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        frameLabel ?: todayLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                JournalFigure(
                    panelModifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth(),
                )
                JournalControls(
                    panelModifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    showReportHeading = false,
                )
            }
        }
    }
}

/** A choice the person can press. The selected one is filled. */
@Composable
private fun InkChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val fill = MaterialTheme.colorScheme.primary
    val onFill = MaterialTheme.colorScheme.onPrimary
    val paper = MaterialTheme.colorScheme.surface
    val ink = MaterialTheme.colorScheme.onSurface
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = GrannyCorner,
        contentPadding = ACTION_PADDING,
        border = if (selected) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) fill else paper,
            contentColor = if (selected) onFill else ink,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = Modifier.height(COMPACT_BUTTON_HEIGHT),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A light text action under the save button. */
@Composable
private fun QuietAction(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Saved check-ins as full-width cards with a larger figure thumb. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember {
        JournalRepository(
            Room.databaseBuilder(
                context.applicationContext,
                JournalDatabase::class.java,
                "journal.db",
            ).build().journalDao(),
        )
    }
    var rows by remember { mutableStateOf<List<JournalObservation>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val formatter = remember {
        DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale.CANADA)
            .withZone(ZoneId.systemDefault())
    }
    fun loadNewestFirst(): List<JournalObservation> =
        repository.current().sortedByDescending { observation -> observation.observedAt }

    LaunchedEffect(Unit) {
        rows = withContext(Dispatchers.IO) { loadNewestFirst() }
    }
    Scaffold(
        modifier = modifier,
        contentWindowInsets = tabDestinationWindowInsets(),
        topBar = {
            TopAppBar(
                title = {
                    BrandHeading(title = stringResource(R.string.journal_open_history))
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (rows.isEmpty()) {
                Text(
                    text = stringResource(R.string.journal_history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = 6.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(HISTORY_ROW_GAP),
                ) {
                    items(
                        items = rows,
                        key = { observation -> observation.id },
                    ) { observation ->
                        HistoryObservationCard(
                            observation = observation,
                            formatter = formatter,
                            onRemoveMeasurement = { measurement ->
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val liveId = repository.current()
                                            .firstOrNull { row ->
                                                row.measurements.any { item ->
                                                    item.bodyPart == measurement.bodyPart &&
                                                        item.side == measurement.side &&
                                                        item.symptom == measurement.symptom
                                                }
                                            }
                                            ?.id
                                        if (liveId != null) {
                                            repository.deleteMeasurement(
                                                observationId = liveId,
                                                bodyPart = measurement.bodyPart,
                                                side = measurement.side,
                                                symptom = measurement.symptom,
                                                deletedAt = Instant.now(),
                                            )
                                        }
                                    }
                                    rows = withContext(Dispatchers.IO) { loadNewestFirst() }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryObservationCard(
    observation: JournalObservation,
    formatter: DateTimeFormatter,
    onRemoveMeasurement: (JournalMeasurement) -> Unit,
) {
    val panel = MaterialTheme.colorScheme.surfaceContainerLowest
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(panel, RoundedCornerShape(HISTORY_CARD_CORNER))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(HISTORY_ROW_GAP),
    ) {
        observation.measurements.forEach { measurement ->
            HistoryReportRow(
                measurement = measurement,
                observedAt = measurement.previousObservedAt ?: observation.observedAt,
                formatter = formatter,
                onRemove = { onRemoveMeasurement(measurement) },
            )
        }
    }
}

/**
 * Attaches weather context to a Feel save when the forecast cache allows.
 *
 * Journal rows always save; unusable weather simply skips the sample.
 */
private fun recordFeelCalibration(
    forecastRepository: ForecastRepository?,
    feelCalibrationStore: FeelCalibrationStore?,
    observationId: String,
    observedAt: Instant,
    ratings: List<Int>,
) {
    if (forecastRepository == null || feelCalibrationStore == null || ratings.isEmpty()) {
        return
    }
    val capture = forecastRepository.captureFeelWeather(observedAt)
    feelCalibrationStore.recordSave(
        observedAt = observedAt,
        observationId = observationId,
        ratings = ratings,
        capture = capture,
    )
}

@Composable
private fun HistoryReportRow(
    measurement: JournalMeasurement,
    observedAt: Instant,
    formatter: DateTimeFormatter,
    onRemove: () -> Unit,
) {
    val whenLabel = formatter.format(observedAt)
    val label = quietReportTitle(
        measurement.side,
        measurement.bodyPart,
        measurement.symptom,
    )
    val headline = quietHistoryHeadline(
        measurement.side,
        measurement.bodyPart,
        measurement.symptom,
        measurement.rating,
        whenLabel,
    )
    val detail = quietReportDetail(measurement.reportKind)
    val ratingSpoken = stringResource(R.string.journal_rating, measurement.rating)
    val spoken = "$label, $ratingSpoken, $whenLabel, $detail"
    val thumbMark = FigureMark(
        part = measurement.bodyPart,
        side = measurement.side,
        rating = measurement.rating,
        ache = measurement.symptom == "ache",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = spoken
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .height(HISTORY_THUMB_HEIGHT)
                .aspectRatio(BodyMap.WIDTH / BodyMap.HEIGHT),
            contentAlignment = Alignment.Center,
        ) {
            BodyFigure(
                marks = listOf(thumbMark),
                onSelect = { _, _ -> },
                enabled = false,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(
            onClick = onRemove,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        ) {
            Text(
                text = stringResource(R.string.journal_delete),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
