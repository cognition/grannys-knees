package com.example.grannysknees.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.grannysknees.R
import com.example.grannysknees.data.ForecastRepository
import com.example.grannysknees.data.SelectedCityStore
import com.example.grannysknees.journal.HistoryScreen
import com.example.grannysknees.journal.JournalScreen
import com.example.grannysknees.ui.about.AboutScreen
import com.example.grannysknees.ui.about.OnboardingScreen
import com.example.grannysknees.ui.about.OnboardingStore
import com.example.grannysknees.ui.privacy.PrivacyScreen
import com.example.grannysknees.ui.theme.GrannysKneesTheme
import com.example.grannysknees.ui.today.TodayRoute

@Composable
internal fun GrannysKneesApp(
    repository: ForecastRepository? = null,
    cityStore: SelectedCityStore? = null,
    onboardingStore: OnboardingStore? = null,
) {
    GrannysKneesTheme {
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
        Scaffold(
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
                    )
                }
                composable(AppDestination.Home.route) {
                    FoundationShell()
                }
                composable(AppDestination.Journal.route) {
                    JournalScreen()
                }
                composable(AppDestination.History.route) {
                    HistoryScreen()
                }
                composable(AppDestination.Privacy.route) {
                    PrivacyScreen(onBack = { navController.popBackStack() })
                }
                composable(AppDestination.About.route) {
                    AboutScreen(onBack = { navController.popBackStack() })
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
    BottomTab(AppDestination.About, R.drawable.ic_nav_about, R.string.nav_about),
)

@Composable
private fun BottomTabs(
    currentRoute: String?,
    onSelect: (String) -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        bottomTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.destination.route,
                onClick = { onSelect(tab.destination.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ink,
                    selectedTextColor = ink,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
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
