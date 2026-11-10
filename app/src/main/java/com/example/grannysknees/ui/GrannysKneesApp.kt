package com.example.grannysknees.ui

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
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
import com.example.grannysknees.ui.theme.BrandHeading
import com.example.grannysknees.ui.theme.GrannysKneesTheme
import com.example.grannysknees.ui.theme.ThemePreference
import com.example.grannysknees.ui.theme.ThemePreferenceStore
import com.example.grannysknees.ui.today.TodayRoute

/** Landscape icon-only rail: wide enough for a 48 dp touch target. */
private val SideRailWidth = 72.dp

/** Keep landscape rail icons readable on short phone heights. */
private val SideRailIconSize = 24.dp

/** Vertical gap between rail items before scrolling kicks in. */
private val SideRailItemSpacing = 8.dp

@Composable
internal fun GrannysKneesApp(
    repository: ForecastRepository? = null,
    cityStore: SelectedCityStore? = null,
    sensitivityStore: SensitivityProfileStore? = null,
    feelCalibrationStore: FeelCalibrationStore? = null,
    onboardingStore: OnboardingStore? = null,
    themePreferenceStore: ThemePreferenceStore? = null,
    onPresentationChanged: () -> Unit = {},
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
    var localPressureEnabled by remember {
        mutableStateOf(sensitivityStore?.readLocalPressureEnabled() ?: false)
    }
    var sensitivityEpoch by remember { mutableStateOf(0) }
    var journalDataEpoch by remember { mutableIntStateOf(0) }
    LaunchedEffect(sensitivityEpoch) {
        if (sensitivityEpoch == 0) {
            return@LaunchedEffect
        }
        onPresentationChanged()
    }
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
        // Privacy stays inside the About tab shell (same bottom / side bar area).
        val showTabs = currentRoute == null ||
            currentRoute == AppDestination.Privacy.route ||
            bottomTabs.any { it.destination.route == currentRoute }
        val selectedTabRoute = when (currentRoute) {
            AppDestination.Privacy.route -> AppDestination.About.route
            else -> currentRoute
        }
        val onSelectTab: (String) -> Unit = { route ->
            navController.navigate(route) {
                popUpTo(AppDestination.Today.route) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
        val isLandscape =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        // Zero content insets here: the tab chrome owns system-nav padding, and
        // each destination Scaffold owns status-bar / top insets. Applying
        // safeDrawing again on the outer Scaffold doubled the gap above the tabs.
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (showTabs) {
                    SideTabs(
                        currentRoute = selectedTabRoute,
                        onSelect = onSelectTab,
                    )
                }
                AppNavHost(
                    navController = navController,
                    repository = repository,
                    cityStore = cityStore,
                    feelCalibrationStore = feelCalibrationStore,
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
                    localPressureEnabled = localPressureEnabled,
                    onLocalPressureEnabledChange = { enabled ->
                        localPressureEnabled = enabled
                        sensitivityStore?.writeLocalPressureEnabled(enabled)
                        sensitivityEpoch += 1
                    },
                    sensitivityEpoch = sensitivityEpoch,
                    onFeelCleared = { sensitivityEpoch += 1 },
                    journalDataEpoch = journalDataEpoch,
                    onDataReset = {
                        journalDataEpoch += 1
                        sensitivityEpoch += 1
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        } else {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showTabs) {
                        BottomTabs(
                            currentRoute = selectedTabRoute,
                            onSelect = onSelectTab,
                        )
                    }
                },
            ) { innerPadding ->
                AppNavHost(
                    navController = navController,
                    repository = repository,
                    cityStore = cityStore,
                    feelCalibrationStore = feelCalibrationStore,
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
                    localPressureEnabled = localPressureEnabled,
                    onLocalPressureEnabledChange = { enabled ->
                        localPressureEnabled = enabled
                        sensitivityStore?.writeLocalPressureEnabled(enabled)
                        sensitivityEpoch += 1
                    },
                    sensitivityEpoch = sensitivityEpoch,
                    onFeelCleared = { sensitivityEpoch += 1 },
                    journalDataEpoch = journalDataEpoch,
                    onDataReset = {
                        journalDataEpoch += 1
                        sensitivityEpoch += 1
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

/**
 * Shared tab destinations for portrait bottom bar and landscape side rail.
 */
@Composable
private fun AppNavHost(
    navController: NavHostController,
    repository: ForecastRepository?,
    cityStore: SelectedCityStore?,
    feelCalibrationStore: FeelCalibrationStore?,
    themePreference: ThemePreference,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    sensitivityProfile: SensitivityProfile,
    onSensitivityProfileChange: (SensitivityProfile) -> Unit,
    feelCalibrationEnabled: Boolean,
    onFeelCalibrationEnabledChange: (Boolean) -> Unit,
    grannySpeaksFreely: Boolean,
    onGrannySpeaksFreelyChange: (Boolean) -> Unit,
    localPressureEnabled: Boolean,
    onLocalPressureEnabledChange: (Boolean) -> Unit,
    sensitivityEpoch: Int,
    onFeelCleared: () -> Unit,
    journalDataEpoch: Int,
    onDataReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Today.route,
        modifier = modifier,
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
                    onFeelCalibrationEnabledChange = onFeelCalibrationEnabledChange,
                    onFeelSaved = onFeelCleared,
                )
            }
        }
        composable(AppDestination.History.route) {
            key(journalDataEpoch) {
                HistoryScreen()
            }
        }
        composable(AppDestination.Privacy.route) {
            PrivacyScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(AppDestination.About.route) {
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
        composable(AppDestination.Settings.route) {
            SettingsScreen(
                themePreference = themePreference,
                onThemePreferenceChange = onThemePreferenceChange,
                sensitivityProfile = sensitivityProfile,
                onSensitivityProfileChange = onSensitivityProfileChange,
                feelCalibrationEnabled = feelCalibrationEnabled,
                onFeelCalibrationEnabledChange = onFeelCalibrationEnabledChange,
                grannySpeaksFreely = grannySpeaksFreely,
                onGrannySpeaksFreelyChange = onGrannySpeaksFreelyChange,
                localPressureEnabled = localPressureEnabled,
                onLocalPressureEnabledChange = onLocalPressureEnabledChange,
                feelCalibrationStore = feelCalibrationStore,
                onFeelCleared = onFeelCleared,
                onDataReset = onDataReset,
            )
        }
        composable(AppDestination.About.route) {
            AboutScreen(
                onBack = { navController.popBackStack() },
                onOpenPrivacy = {
                    navController.navigate(AppDestination.Privacy.route) {
                        launchSingleTop = true
                    }
                },
            )
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

/**
 * Landscape tab chrome: icons only, inset from system bars, scrollable when short.
 *
 * Labels stay on TalkBack via each icon's content description. Start/top/bottom
 * [WindowInsets.safeDrawing] keep items clear of cutouts and the system nav /
 * gesture bar (Galaxy landscape especially). Fixed icon size avoids unreadably
 * tiny targets; scroll when vertical space is tight after insets.
 */
@Composable
private fun SideTabs(
    currentRoute: String?,
    onSelect: (String) -> Unit,
) {
    val selected = MaterialTheme.colorScheme.primary
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxHeight(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Start +
                            WindowInsetsSides.Top +
                            WindowInsetsSides.Bottom,
                    ),
                )
                .width(SideRailWidth),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(SideRailItemSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                bottomTabs.forEach { tab ->
                    val isSelected = currentRoute == tab.destination.route
                    IconButton(onClick = { onSelect(tab.destination.route) }) {
                        Icon(
                            painter = painterResource(tab.icon),
                            contentDescription = stringResource(tab.label),
                            modifier = Modifier.size(SideRailIconSize),
                            tint = if (isSelected) selected else quiet,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoundationShell() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { BrandHeading(title = AppDestination.Home.title) },
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
