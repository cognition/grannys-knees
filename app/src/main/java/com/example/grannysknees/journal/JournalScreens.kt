package com.example.grannysknees.journal

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.grannysknees.R
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
private val NOTICE_HEIGHT = 24.dp
private val ACTION_PADDING = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

/** Partial symptom entry. Chosen places add together, and only those are written. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun JournalScreen(
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
            weekFrames(repository.all(), Instant.now(), zone)
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
        if (!editing) {
            return
        }
        val existing = chosen.indexOfFirst { it.part == part && it.side == side }
        if (existing >= 0) {
            focus = existing
        } else {
            val current = chosen[focus]
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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.journal_open)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.journal_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val reportTitle = if (hasPlace) {
                quietReportTitle(place.side, place.part, place.symptom)
            } else {
                stringResource(R.string.journal_day_empty)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                frameLabel ?: todayLabel,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 320.dp),
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
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(
                            BodyMap.WIDTH / BodyMap.HEIGHT,
                            matchHeightConstraintsFirst = true,
                        ),
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
            Column(modifier = Modifier.height(24.dp)) {
                if (displayed.size > 1) {
                    Text(
                        displayed.joinToString(separator = ", ") { item ->
                            quietReportTitle(item.side, item.part, item.symptom)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                if (hasPlace) {
                    stringResource(R.string.journal_rating, place.rating)
                } else {
                    stringResource(R.string.journal_rating_empty)
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val ink = MaterialTheme.colorScheme.onSurface
            val quietLine = MaterialTheme.colorScheme.outlineVariant
            val ratingDescription = if (hasPlace) {
                stringResource(R.string.journal_rating, place.rating)
            } else {
                stringResource(R.string.journal_rating_empty)
            }
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
                    thumbColor = ink,
                    activeTrackColor = ink,
                    inactiveTrackColor = quietLine,
                    activeTickColor = MaterialTheme.colorScheme.surface,
                    inactiveTickColor = ink.copy(alpha = 0.35f),
                ),
                modifier = Modifier.semantics {
                    contentDescription = ratingDescription
                },
            )
            Button(
                onClick = {
                    scope.launch {
                        val edits = chosen.map { item ->
                            SeriesEdit(item.part, item.side, item.symptom, item.rating)
                        }
                        val wrote = withContext(Dispatchers.IO) {
                            repository.save(
                                committedAt = Instant.now(),
                                edits = edits,
                                confirmations = emptyList(),
                            )
                        }
                        notice = if (wrote == null) nothing else saved
                    }
                },
                enabled = editing,
                shape = GrannyCorner,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp),
            ) { Text(stringResource(R.string.journal_save_change)) }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActionButton(
                    label = stringResource(
                        if (playing) R.string.journal_stop_week else R.string.journal_play_week,
                    ),
                    onClick = { playing = !playing },
                )
                ActionButton(
                    label = stringResource(R.string.journal_same_as_yesterday),
                    enabled = editing,
                    onClick = {
                        if (!editing) {
                            return@ActionButton
                        }
                        scope.launch {
                            val places = withContext(Dispatchers.IO) {
                                placesFromYesterday(repository.all(), Instant.now(), zone)
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
                ActionButton(
                    label = stringResource(R.string.journal_unchanged),
                    enabled = editing,
                    onClick = {
                        if (!editing) {
                            return@ActionButton
                        }
                        scope.launch {
                            val keys = chosen.map { item ->
                                SeriesKey(item.part, item.side, item.symptom)
                            }
                            val wrote = withContext(Dispatchers.IO) {
                                repository.save(
                                    committedAt = Instant.now(),
                                    edits = emptyList(),
                                    confirmations = keys,
                                )
                            }
                            notice = if (wrote == null) nothing else saved
                        }
                    },
                )
            }
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
    val ink = MaterialTheme.colorScheme.onSurface
    val paper = MaterialTheme.colorScheme.surface
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
            containerColor = if (selected) ink else paper,
            contentColor = if (selected) paper else ink,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = Modifier.heightIn(min = 40.dp),
    ) {
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A secondary action, filled so it reads as a button. */
@Composable
private fun ActionButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val paper = MaterialTheme.colorScheme.surface
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = GrannyCorner,
        contentPadding = ACTION_PADDING,
        colors = ButtonDefaults.buttonColors(
            containerColor = ink,
            contentColor = paper,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = Modifier.heightIn(min = 40.dp),
    ) {
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Current reports, with correction, deletion, export, and a full wipe. */
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
        DateTimeFormatter.ofPattern("EEE HH:mm", Locale.CANADA).withZone(ZoneId.systemDefault())
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        rows = withContext(Dispatchers.IO) { repository.current() }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.journal_open_history)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            if (rows.isEmpty()) {
                Text(stringResource(R.string.journal_history_empty))
            }
            rows.forEach { observation ->
                observation.measurements.forEach { measurement ->
                    val whenLabel = formatter.format(measurement.previousObservedAt ?: observation.observedAt)
                    Text(
                        quietReportTitle(measurement.side, measurement.bodyPart, measurement.symptom),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        quietReportDetail(measurement.rating, measurement.reportKind, whenLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ActionButton(
                        label = stringResource(R.string.journal_correct),
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    repository.save(
                                        committedAt = Instant.now(),
                                        edits = listOf(
                                            SeriesEdit(
                                                measurement.bodyPart,
                                                measurement.side,
                                                measurement.symptom,
                                                measurement.rating,
                                            ),
                                        ),
                                        confirmations = emptyList(),
                                        correctsObservationId = observation.id,
                                    )
                                }
                                rows = withContext(Dispatchers.IO) { repository.current() }
                            }
                        },
                    )
                }
                ActionButton(
                    label = stringResource(R.string.journal_delete),
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                repository.deleteReport(observation.id, Instant.now())
                            }
                            rows = withContext(Dispatchers.IO) { repository.current() }
                        }
                    },
                )
            }
            Button(
                onClick = {
                    scope.launch {
                        val payload = withContext(Dispatchers.IO) { repository.exportCurrentJson() }
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_TEXT, payload)
                        }
                        context.startActivity(Intent.createChooser(share, null))
                    }
                },
                shape = GrannyCorner,
            ) { Text(stringResource(R.string.journal_export)) }
            ActionButton(
                label = stringResource(R.string.journal_wipe),
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { repository.wipe() }
                        rows = emptyList()
                    }
                },
            )
        }
    }
}
