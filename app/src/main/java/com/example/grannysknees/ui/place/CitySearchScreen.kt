package com.example.grannysknees.ui.place

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.grannysknees.R
import com.example.grannysknees.data.SelectedCity
import com.example.grannysknees.location.OptionalGpsPlaceSource
import com.example.grannysknees.ui.tabDestinationWindowInsets
import com.example.grannysknees.ui.theme.BrandHeading
import com.example.grannysknees.weather.OpenMeteoGeocodingClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Asks for a Canadian city before any forecast is fetched.
 * GPS is offered and can be declined without blocking the search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CitySearchScreen(
    geocoder: OpenMeteoGeocodingClient,
    onCityChosen: (SelectedCity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val gps = remember { OptionalGpsPlaceSource(context) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SelectedCity>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    val noResults = stringResource(R.string.place_no_results)
    val failed = stringResource(R.string.place_search_failed)
    val locationFailed = stringResource(R.string.place_location_failed)
    val scope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        val allowed = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!allowed) {
            message = locationFailed
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val city = withContext(Dispatchers.IO) { gps.currentCity() }
            if (city == null) {
                message = locationFailed
            } else {
                onCityChosen(city)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = tabDestinationWindowInsets(),
        topBar = {
            TopAppBar(
                title = {
                    BrandHeading(title = stringResource(R.string.place_title))
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.place_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.place_search_label)) },
                singleLine = true,
            )
            Button(
                onClick = {
                    scope.launch {
                        val found = try {
                            withContext(Dispatchers.IO) { geocoder.search(query) }
                        } catch (_: IOException) {
                            results = emptyList()
                            message = failed
                            return@launch
                        }
                        results = found
                        message = if (found.isEmpty()) noResults else null
                    }
                },
            ) {
                Text(stringResource(R.string.place_search_action))
            }
            TextButton(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                },
            ) {
                Text(stringResource(R.string.place_use_location))
            }
            if (message != null) {
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            results.forEach { city ->
                Text(
                    text = "${city.label}, Canada",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCityChosen(city) }
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
}
