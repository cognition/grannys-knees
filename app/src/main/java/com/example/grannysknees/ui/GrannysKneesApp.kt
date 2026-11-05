package com.example.grannysknees.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.grannysknees.data.ForecastRepository
import com.example.grannysknees.data.SelectedCityStore
import com.example.grannysknees.journal.HistoryScreen
import com.example.grannysknees.journal.JournalScreen
import com.example.grannysknees.ui.about.AboutScreen
import com.example.grannysknees.ui.about.OnboardingScreen
import com.example.grannysknees.ui.about.OnboardingStore
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
        NavHost(
            navController = navController,
            startDestination = AppDestination.Today.route,
        ) {
            composable(AppDestination.Today.route) {
                TodayRoute(
                    repository = repository,
                    cityStore = cityStore,
                    onOpenJournal = { navController.navigate(AppDestination.Journal.route) },
                    onOpenHistory = { navController.navigate(AppDestination.History.route) },
                    onOpenAbout = { navController.navigate(AppDestination.About.route) },
                )
            }
            composable(AppDestination.Home.route) {
                FoundationShell()
            }
            composable(AppDestination.Journal.route) {
                JournalScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.History.route) {
                HistoryScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.About.route) {
                AboutScreen(onBack = { navController.popBackStack() })
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
