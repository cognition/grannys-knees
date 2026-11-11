package com.aetherealmind.grannysknees.ui.about

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import androidx.room.Room
import com.aetherealmind.grannysknees.R
import com.aetherealmind.grannysknees.data.FeelCalibrationStore
import com.aetherealmind.grannysknees.data.SensitivityProfile
import com.aetherealmind.grannysknees.journal.JournalDatabase
import com.aetherealmind.grannysknees.journal.JournalRepository
import com.aetherealmind.grannysknees.ui.tabDestinationWindowInsets
import com.aetherealmind.grannysknees.ui.theme.BrandHeading
import com.aetherealmind.grannysknees.ui.theme.GrannyCorner
import com.aetherealmind.grannysknees.ui.theme.ThemePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Remembers that the first-run explanation has been shown. */
internal class OnboardingStore(
    private val file: File,
) {
    fun seen(): Boolean = file.exists()

    fun markSeen() {
        file.parentFile?.mkdirs()
        file.writeText("seen", Charsets.UTF_8)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OnboardingScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    BrandHeading(title = stringResource(R.string.onboarding_title))
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_body),
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onContinue) {
                Text(stringResource(R.string.onboarding_continue))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutScreen(
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = tabDestinationWindowInsets(),
        topBar = {
            TopAppBar(
                title = {
                    BrandHeading(title = stringResource(R.string.journal_open_about))
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DisclaimerCard()
            Text(
                text = stringResource(R.string.about_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.about_granny_says),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.about_body_not_today),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontStyle = FontStyle.Italic,
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            CalculationBlurb()
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = stringResource(R.string.about_open_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpenPrivacy) {
                Text(stringResource(R.string.about_open_privacy_action))
            }
        }
    }
}

/**
 * Bottom-tab Settings: personal calibration, appearance, and local data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    themePreference: ThemePreference,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    sensitivityProfile: SensitivityProfile = SensitivityProfile.DEFAULT,
    onSensitivityProfileChange: (SensitivityProfile) -> Unit = {},
    feelCalibrationEnabled: Boolean = true,
    onFeelCalibrationEnabledChange: (Boolean) -> Unit = {},
    grannySpeaksFreely: Boolean = false,
    onGrannySpeaksFreelyChange: (Boolean) -> Unit = {},
    localPressureEnabled: Boolean = false,
    onLocalPressureEnabledChange: (Boolean) -> Unit = {},
    feelCalibrationStore: FeelCalibrationStore? = null,
    onFeelCleared: () -> Unit = {},
    onDataReset: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember {
        JournalRepository(
            Room.databaseBuilder(
                context.applicationContext,
                JournalDatabase::class.java,
                "journal.db",
            ).build().journalDao(),
        )
    }
    var resetNotice by remember { mutableStateOf<String?>(null) }
    var feelSampleCount by remember {
        mutableStateOf(feelCalibrationStore?.read()?.size ?: 0)
    }
    Scaffold(
        modifier = modifier,
        contentWindowInsets = tabDestinationWindowInsets(),
        topBar = {
            TopAppBar(
                title = {
                    BrandHeading(title = stringResource(R.string.settings_title))
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PersonalCalibrationSection(
                profile = sensitivityProfile,
                onProfileChange = onSensitivityProfileChange,
                feelCalibrationEnabled = feelCalibrationEnabled,
                onFeelCalibrationEnabledChange = onFeelCalibrationEnabledChange,
                feelSampleCount = feelSampleCount,
                onClearFeel = {
                    feelCalibrationStore?.clear()
                    feelSampleCount = 0
                    onFeelCleared()
                    resetNotice = context.getString(R.string.about_feel_clear_done)
                },
            )
            LocalPressureSection(
                enabled = localPressureEnabled,
                onEnabledChange = onLocalPressureEnabledChange,
            )
            FreeSpeechSection(
                enabled = grannySpeaksFreely,
                onEnabledChange = onGrannySpeaksFreelyChange,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AppearanceSection(
                preference = themePreference,
                onPreferenceChange = onThemePreferenceChange,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = stringResource(R.string.about_data_heading),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Button(
                onClick = {
                    scope.launch {
                        val payload = withContext(Dispatchers.IO) {
                            repository.exportCurrentJson()
                        }
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_TEXT, payload)
                        }
                        context.startActivity(Intent.createChooser(share, null))
                    }
                },
                shape = GrannyCorner,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.about_export_reports))
            }
            Text(
                text = stringResource(R.string.about_reset_all_data_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            repository.wipe()
                            feelCalibrationStore?.clear()
                            File(context.filesDir, "report-dummy.json").delete()
                        }
                        onDataReset()
                        resetNotice = context.getString(R.string.about_reset_done)
                    }
                },
            ) {
                Text(stringResource(R.string.about_reset_all_data))
            }
            resetNotice?.let { notice ->
                Text(
                    text = notice,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LocalPressureSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.about_local_pressure_title),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(R.string.about_local_pressure_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Composable
private fun FreeSpeechSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.about_granny_speech_heading),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.about_granny_speech_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.about_granny_speech_title),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
    }
}

/** Plain-language summary of baseline and personal index math. */
@Composable
private fun CalculationBlurb() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.about_calculation_heading),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.about_calculation_body),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Pressure / humidity / cold real-feel ratings (UI 0–10) for the personal
 * index. Stored multipliers stay 0–2 under the hood.
 */
@Composable
private fun PersonalCalibrationSection(
    profile: SensitivityProfile,
    onProfileChange: (SensitivityProfile) -> Unit,
    feelCalibrationEnabled: Boolean,
    onFeelCalibrationEnabledChange: (Boolean) -> Unit,
    feelSampleCount: Int = 0,
    onClearFeel: () -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.about_sensitivity_heading),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.about_sensitivity_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.about_feel_tune_title),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = stringResource(R.string.about_feel_tune_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = feelCalibrationEnabled,
                onCheckedChange = onFeelCalibrationEnabledChange,
            )
        }
        if (feelSampleCount > 0) {
            Text(
                text = stringResource(R.string.about_feel_clear_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onClearFeel) {
                Text(stringResource(R.string.about_feel_clear))
            }
        }
        CalibrationSlider(
            label = stringResource(R.string.about_sensitivity_pressure),
            multiplier = profile.pressure,
            onMultiplierChange = { next ->
                onProfileChange(profile.copy(pressure = next).clamped())
            },
        )
        CalibrationSlider(
            label = stringResource(R.string.about_sensitivity_humidity),
            multiplier = profile.humidity,
            onMultiplierChange = { next ->
                onProfileChange(profile.copy(humidity = next).clamped())
            },
        )
        CalibrationSlider(
            label = stringResource(R.string.about_sensitivity_cold),
            multiplier = profile.cold,
            onMultiplierChange = { next ->
                onProfileChange(profile.copy(cold = next).clamped())
            },
        )
        if (!profile.isDefault()) {
            TextButton(onClick = { onProfileChange(SensitivityProfile.DEFAULT) }) {
                Text(stringResource(R.string.about_sensitivity_reset))
            }
        }
    }
}

@Composable
private fun CalibrationSlider(
    label: String,
    multiplier: Float,
    onMultiplierChange: (Float) -> Unit,
) {
    val displayValue = SensitivityProfile.toDisplayScale(multiplier)
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = displayValue.toInt().toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = displayValue,
            onValueChange = { next ->
                onMultiplierChange(SensitivityProfile.fromDisplayScale(next))
            },
            valueRange = SensitivityProfile.DISPLAY_MIN..SensitivityProfile.DISPLAY_MAX,
            steps = SensitivityProfile.DISPLAY_STEPS,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.about_sensitivity_scale_low),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.about_sensitivity_scale_high),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Light / dark / match-phone controls for the lake-teal theme. */
@Composable
private fun AppearanceSection(
    preference: ThemePreference,
    onPreferenceChange: (ThemePreference) -> Unit,
) {
    val groupLabel = stringResource(R.string.about_theme_group)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.about_appearance_heading),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.about_appearance_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .semantics { contentDescription = groupLabel },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeChoiceChip(
                label = stringResource(R.string.about_theme_system),
                selected = preference == ThemePreference.SYSTEM,
                onClick = { onPreferenceChange(ThemePreference.SYSTEM) },
                modifier = Modifier.weight(1f),
            )
            ThemeChoiceChip(
                label = stringResource(R.string.about_theme_light),
                selected = preference == ThemePreference.LIGHT,
                onClick = { onPreferenceChange(ThemePreference.LIGHT) },
                modifier = Modifier.weight(1f),
            )
            ThemeChoiceChip(
                label = stringResource(R.string.about_theme_dark),
                selected = preference == ThemePreference.DARK,
                onClick = { onPreferenceChange(ThemePreference.DARK) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ThemeChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
    )
}


/** Short not-medical-advice notice for the About screen. */
@Composable
private fun DisclaimerCard() {
    val spoken = stringResource(R.string.about_disclaimer_body)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = spoken },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.about_disclaimer_short),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.about_disclaimer_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.about_disclaimer_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
