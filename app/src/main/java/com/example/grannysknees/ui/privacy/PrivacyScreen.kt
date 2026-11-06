package com.example.grannysknees.ui.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.grannysknees.R

/**
 * Plain-language explanation of the local-first privacy design.
 *
 * This is not a substitute for the externally hosted, publisher-approved
 * privacy policy required by Google Play.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.privacy_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.privacy_intro),
                style = MaterialTheme.typography.bodyLarge,
            )
            PrivacySection(R.string.privacy_weather_heading, R.string.privacy_weather_body)
            PrivacySection(R.string.privacy_local_heading, R.string.privacy_local_body)
            PrivacySection(R.string.privacy_developer_heading, R.string.privacy_developer_body)
            PrivacySection(R.string.privacy_provider_heading, R.string.privacy_provider_body)
            PrivacySection(R.string.privacy_watch_heading, R.string.privacy_watch_body)
            PrivacySection(R.string.privacy_backup_heading, R.string.privacy_backup_body)
            Text(
                text = stringResource(R.string.privacy_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PrivacySection(heading: Int, body: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = stringResource(heading), style = MaterialTheme.typography.titleMedium)
        Text(text = stringResource(body), style = MaterialTheme.typography.bodyMedium)
    }
}
