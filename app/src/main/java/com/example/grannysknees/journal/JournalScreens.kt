package com.example.grannysknees.journal

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.grannysknees.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val BODY_PARTS: List<String> = listOf(
    "head", "neck", "shoulder", "elbow", "wrist", "hand",
    "back", "hip", "knee", "ankle", "foot", "other",
)
private val SIDES: List<String?> = listOf(null, "left", "right")
private val SYMPTOMS: List<String> = listOf("ache", "stiffness")

/** Partial symptom entry. Only the series the user saves is written. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JournalScreen(
    onBack: () -> Unit,
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
    var partIndex by remember { mutableIntStateOf(9) }
    var sideIndex by remember { mutableIntStateOf(1) }
    var symptomIndex by remember { mutableIntStateOf(0) }
    var rating by remember { mutableFloatStateOf(0f) }
    var notice by remember { mutableStateOf<String?>(null) }
    val saved = stringResource(R.string.journal_saved)
    val nothing = stringResource(R.string.journal_nothing_saved)
    val scope = rememberCoroutineScope()
    val part = BODY_PARTS[partIndex]
    val side = SIDES[sideIndex]
    val symptom = SYMPTOMS[symptomIndex]
    val sideLabel = side ?: "none"

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.journal_open)) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.journal_intro), style = MaterialTheme.typography.bodyMedium)
            TextButton(
                onClick = { partIndex = (partIndex + 1) % BODY_PARTS.size },
                modifier = Modifier.semantics {
                    contentDescription = "Body part $part"
                },
            ) { Text(stringResource(R.string.journal_body_part, part)) }
            TextButton(onClick = { sideIndex = (sideIndex + 1) % SIDES.size }) {
                Text(stringResource(R.string.journal_side, sideLabel))
            }
            TextButton(onClick = { symptomIndex = (symptomIndex + 1) % SYMPTOMS.size }) {
                Text(stringResource(R.string.journal_symptom, symptom))
            }
            Text(stringResource(R.string.journal_rating, rating.toInt()))
            Slider(
                value = rating,
                onValueChange = { rating = it },
                valueRange = 0f..10f,
                steps = 9,
                modifier = Modifier.semantics { contentDescription = "Rating ${rating.toInt()} out of 10" },
            )
            Button(
                onClick = {
                    scope.launch {
                        val wrote = withContext(Dispatchers.IO) {
                            repository.save(
                                committedAt = Instant.now(),
                                edits = listOf(
                                    SeriesEdit(part, side, symptom, rating.toInt()),
                                ),
                                confirmations = emptyList(),
                            )
                        }
                        notice = if (wrote == null) nothing else saved
                    }
                },
            ) { Text(stringResource(R.string.journal_save_change)) }
            TextButton(
                onClick = {
                    scope.launch {
                        val wrote = withContext(Dispatchers.IO) {
                            repository.save(
                                committedAt = Instant.now(),
                                edits = emptyList(),
                                confirmations = listOf(SeriesKey(part, side, symptom)),
                            )
                        }
                        notice = if (wrote == null) nothing else saved
                    }
                },
            ) { Text(stringResource(R.string.journal_confirm)) }
            if (notice != null) {
                Text(notice.orEmpty())
            }
        }
    }
}

/** Current reports, with correction, deletion, export, and a full wipe. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryScreen(
    onBack: () -> Unit,
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
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        rows = withContext(Dispatchers.IO) { repository.current() }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.journal_open_history)) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (rows.isEmpty()) {
                Text(stringResource(R.string.journal_history_empty))
            }
            rows.forEach { observation ->
                observation.measurements.forEach { measurement ->
                    val whenLabel = formatter.format(measurement.previousObservedAt ?: observation.observedAt)
                    Text(
                        "${measurement.bodyPart} ${measurement.side ?: ""} ${measurement.symptom}: " +
                            "${measurement.rating} (${measurement.reportKind})",
                    )
                    Text(stringResource(R.string.journal_last_reported, whenLabel))
                    TextButton(
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
                    ) { Text(stringResource(R.string.journal_correct)) }
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                repository.deleteReport(observation.id, Instant.now())
                            }
                            rows = withContext(Dispatchers.IO) { repository.current() }
                        }
                    },
                ) { Text(stringResource(R.string.journal_delete)) }
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
            ) { Text(stringResource(R.string.journal_export)) }
            TextButton(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { repository.wipe() }
                        rows = emptyList()
                    }
                },
            ) { Text(stringResource(R.string.journal_wipe)) }
        }
    }
}
