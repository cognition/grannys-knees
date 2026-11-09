package com.example.grannysknees.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.grannysknees.R
import com.example.grannysknees.data.FeelCalibrationStore
import com.example.grannysknees.data.ForecastRepository
import com.example.grannysknees.data.SelectedCityStore
import com.example.grannysknees.data.SensitivityProfile
import com.example.grannysknees.data.SensitivityProfileStore
import com.example.grannysknees.journal.HistoryScreen
import com.example.grannysknees.journal.JournalScreen
import com.example.grannysknees.ui.about.AboutScreen
import com.example.grannysknees.ui.about.OnboardingScreen
import com.example.grannysknees.ui.about.OnboardingStore
import com.example.grannysknees.ui.about.SettingsScreen
import com.example.grannysknees.ui.privacy.PrivacyScreen
import com.example.grannysknees.ui.theme.GrannysKneesTheme
import com.example.grannysknees.ui.theme.ThemePreference
import com.example.grannysknees.ui.theme.ThemePreferenceStore
import com.example.grannysknees.ui.today.TodayRoute

@Composable
internal fun GrannysKneesApp(
    repository: ForecastRepository? = null,
    cityStore: SelectedCityStore? = null,
    sensitivityStore: SensitivityProfileStore? = null,
    feelCalibrationStore: FeelCalibrationStore? = null,
    onboardingStore: OnboardingStore? = null,
    themePreferenceStore: ThemePreferenceStore? = null,
) {
    val systemDark = isSystemInDarkTheme()
    var themePreference by remember {
        mutableStateOf(themePreferenceStore?.read() ?: ThemePreference.SYSTEM)
    }
    var sensitivityProfile by remember {
        mutableStateOf(sensitivityStore?.read() ?: SensitivityProfile.DEFAULT)
    }
    var feelCalibrationEnabled by remember {
        mutableStateOf(sensitivityStore?.readFeelCalibrationEnabled() ?: true)
    }
    var grannySpeaksFreely by remember {
        mutableStateOf(sensitivityStore?.readGrannySpeaksFreely() ?: false)
    }
    var sensitivityEpoch by remember { mutableStateOf(0) }
    var journalDataEpoch by remember { mutableIntStateOf(0) }
    GrannysKneesTheme(darkTheme = themePreference.resolveDark(systemDark)) {
        var showOnboarding by remember { mutableStateOf(onboardingStore?.seen() == false) }
        if (showOnboarding && onboardingStore != null) {
            OnboardingScreen(
                onContinue = {
                    onboardingStore.markSeen()
                    showOnboarding = false
                },
            )
            return@GrannysKneesTheme
        }
        val navController = rememberNavController()
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        val showTabs = currentRoute == null || bottomTabs.any { it.destination.route == currentRoute }
        // Zero content insets here: the bottom bar owns system-nav padding, and
        // each destination Scaffold owns status-bar / top insets. Applying
        // safeDrawing again on the outer Scaffold doubled the gap above the tabs.
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showTabs) {
                    BottomTabs(
                        currentRoute = currentRoute,
                        onSelect = { route ->
                            navController.navigate(route) {
                                popUpTo(AppDestination.Today.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppDestination.Today.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(AppDestination.Today.route) {
                    TodayRoute(
                        repository = repository,
                        cityStore = cityStore,
                        sensitivityEpoch = sensitivityEpoch,
                        grannySpeaksFreely = grannySpeaksFreely,
                    )
                }
                composable(AppDestination.Home.route) {
                    FoundationShell()
                }
                composable(AppDestination.Journal.route) {
                    key(journalDataEpoch) {
                        JournalScreen(
                            repository = repository,
                            feelCalibrationStore = feelCalibrationStore,
                            feelCalibrationEnabled = feelCalibrationEnabled,
                            onFeelCalibrationEnabledChange = { enabled ->
                                feelCalibrationEnabled = enabled
                                sensitivityStore?.writeFeelCalibrationEnabled(enabled)
                                sensitivityEpoch += 1
                            },
                            onFeelSaved = { sensitivityEpoch += 1 },
                        )
                    }
                }
                composable(AppDestination.History.route) {
                    key(journalDataEpoch) {
                        HistoryScreen()
                    }
                }
                composable(AppDestination.Privacy.route) {
                    PrivacyScreen(onBack = { navController.popBackStack() })
                }
                composable(AppDestination.Settings.route) {
                    SettingsScreen(
                        themePreference = themePreference,
                        onThemePreferenceChange = { next ->
                            themePreference = next
                            themePreferenceStore?.write(next)
                        },
                        sensitivityProfile = sensitivityProfile,
                        onSensitivityProfileChange = { next ->
                            sensitivityProfile = next
                            sensitivityStore?.write(next)
                            sensitivityEpoch += 1
                        },
                        feelCalibrationEnabled = feelCalibrationEnabled,
                        onFeelCalibrationEnabledChange = { enabled ->
                            feelCalibrationEnabled = enabled
                            sensitivityStore?.writeFeelCalibrationEnabled(enabled)
                            sensitivityEpoch += 1
                        },
                        grannySpeaksFreely = grannySpeaksFreely,
                        onGrannySpeaksFreelyChange = { enabled ->
                            grannySpeaksFreely = enabled
                            sensitivityStore?.writeGrannySpeaksFreely(enabled)
                        },
                        feelCalibrationStore = feelCalibrationStore,
                        onDataReset = { journalDataEpoch += 1 },
                    )
                }
                composable(AppDestination.About.route) {
                    AboutScreen(
                        onBack = { navController.popBackStack() },
                        onOpenPrivacy = {
                            navController.navigate(AppDestination.Privacy.route)
                        },
                    )
                }
            }
        }
    }
}


private data class BottomTab(
    val destination: AppDestination,
    val icon: Int,
    val label: Int,
)

private val bottomTabs: List<BottomTab> = listOf(
    BottomTab(AppDestination.Today, R.drawable.ic_nav_today, R.string.nav_today),
    BottomTab(AppDestination.Journal, R.drawable.ic_nav_report, R.string.nav_report),
    BottomTab(AppDestination.History, R.drawable.ic_nav_reports, R.string.nav_reports),
    BottomTab(AppDestination.Settings, R.drawable.ic_nav_settings, R.string.nav_settings),
    BottomTab(AppDestination.About, R.drawable.ic_nav_about, R.string.nav_about),
)

@Composable
private fun BottomTabs(
    currentRoute: String?,
    onSelect: (String) -> Unit,
) {
    val selected = MaterialTheme.colorScheme.primary
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        bottomTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.destination.route,
                onClick = { onSelect(tab.destination.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = selected,
                    selectedTextColor = selected,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = quiet,
                    unselectedTextColor = quiet,
                ),

                icon = {
                    Icon(
                        painter = painterResource(tab.icon),
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoundationShell() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(AppDestination.Home.title) },
            )
        },
    ) { innerPadding ->
        EmptyContent(innerPadding)
    }
}

@Composable
private fun EmptyContent(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Native Android foundation",
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Product features will arrive in later delivery tickets.",
                textAlign = TextAlign.Center,
            )
        }
    }
}
