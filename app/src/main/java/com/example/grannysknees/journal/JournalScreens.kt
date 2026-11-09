package com.example.grannysknees.journal

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.grannysknees.ui.tabDestinationWindowInsets
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.grannysknees.R
import com.example.grannysknees.data.FeelCalibrationStore
import com.example.grannysknees.data.ForecastRepository
import com.example.grannysknees.ui.theme.GrannyCorner
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
private val HISTORY_THUMB_HEIGHT = 104.dp
private val HISTORY_CARD_CORNER = 10.dp

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
    val place = displayed.getOrNull(displayedFocus) ?: defaultReportPlaces().first()
    val hasPlace = displayed.isNotEmpty()
    fun addOrFocus(part: String, side: String?) {
        if (!editing || chosen.isEmpty()) {
            return
        }
        val existing = chosen.indexOfFirst { it.part == part && it.side == side }
        if (existing >= 0) {
            focus = existing
        } else {
            val current = chosen.getOrNull(focus.coerceIn(0, chosen.lastIndex)) ?: return
            chosen = chosen + ChosenPlace(part, side, current.symptom, current.rating)
            focus = chosen.lastIndex
        }
    }
    fun replaceFocus(next: ChosenPlace) {
        if (!editing) {
            return
        }
        chosen = chosen.mapIndexed { index, item -> if (index == focus) next else item }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = tabDestinationWindowInsets(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.journal_open)) },
            )
        },
    ) { innerPadding ->
        val reportTitle = if (hasPlace) {
            quietReportTitle(place.side, place.part, place.symptom)
        } else {
            stringResource(R.string.journal_day_empty)
        }
        val accent = MaterialTheme.colorScheme.primary
        val quietLine = MaterialTheme.colorScheme.outlineVariant
        val ratingDescription = if (hasPlace) {
            stringResource(R.string.journal_rating, place.rating)
        } else {
            stringResource(R.string.journal_rating_empty)
        }
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
                        if (hasPlace) {
                            stringResource(grannyAside(place.part, place.side))
                        } else {
                            "\u00a0"
                        },
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
            Box(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .fillMaxWidth(),
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
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SYMPTOMS.forEach { option ->
                    InkChoice(
                        label = option.replaceFirstChar { it.titlecase() },
                        selected = hasPlace && place.symptom == option,
                        enabled = editing && hasPlace,
                        onClick = { replaceFocus(place.copy(symptom = option)) },
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
                if (hasPlace) {
                    stringResource(R.string.journal_rating, place.rating)
                } else {
                    stringResource(R.string.journal_rating_empty)
                },
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Slider(
                value = if (hasPlace) place.rating.toFloat() else 0f,
                onValueChange = { value ->
                    if (!editing || !hasPlace) {
                        return@Slider
                    }
                    chosen = chosen.mapIndexed { index, item ->
                        if (index == focus) item.copy(rating = value.toInt()) else item
                    }
                },
                enabled = editing && hasPlace,
                valueRange = 0f..10f,
                steps = 9,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.journal_feel_tune_title),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = stringResource(
                            if (feelCalibrationEnabled) {
                                R.string.journal_feel_tune_hint_on
                            } else {
                                R.string.journal_feel_tune_hint_off
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = feelCalibrationEnabled,
                    onCheckedChange = onFeelCalibrationEnabledChange,
                )
            }
            Button(
                onClick = {
                    scope.launch {
                        val edits = chosen.map { item ->
                            SeriesEdit(item.part, item.side, item.symptom, item.rating)
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
                                chosen = places
                                focus = places.lastIndex
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
    LaunchedEffect(Unit) {
        rows = withContext(Dispatchers.IO) { repository.current() }
    }
    Scaffold(
        modifier = modifier,
        contentWindowInsets = tabDestinationWindowInsets(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.journal_open_history)) },
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
                        horizontal = 14.dp,
                        vertical = 10.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = rows,
                        key = { observation -> observation.id },
                    ) { observation ->
                        HistoryObservationCard(
                            observation = observation,
                            formatter = formatter,
                            onRemove = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        repository.deleteReport(observation.id, Instant.now())
                                    }
                                    rows = withContext(Dispatchers.IO) { repository.current() }
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
    onRemove: () -> Unit,
) {
    val panel = MaterialTheme.colorScheme.surfaceContainerLowest
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(panel, RoundedCornerShape(HISTORY_CARD_CORNER))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        observation.measurements.forEach { measurement ->
            HistoryReportRow(
                measurement = measurement,
                observedAt = measurement.previousObservedAt ?: observation.observedAt,
                formatter = formatter,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = onRemove,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = stringResource(R.string.journal_delete),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
) {
    val whenLabel = formatter.format(observedAt)
    val thumbMark = FigureMark(
        part = measurement.bodyPart,
        side = measurement.side,
        rating = measurement.rating,
        ache = measurement.symptom == "ache",
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
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
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = quietReportTitle(
                    measurement.side,
                    measurement.bodyPart,
                    measurement.symptom,
                ),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = quietReportDetail(
                    measurement.rating,
                    measurement.reportKind,
                    whenLabel,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
