package com.example.grannysknees

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.grannysknees.data.DailyFetchStampStore
import com.example.grannysknees.data.FeelCalibrationStore
import com.example.grannysknees.data.ForecastCacheStore
import com.example.grannysknees.data.ForecastRepository
import com.example.grannysknees.data.SelectedCityStore
import com.example.grannysknees.data.SensitivityProfileStore
import com.example.grannysknees.ui.GrannysKneesApp
import com.example.grannysknees.ui.about.OnboardingStore
import com.example.grannysknees.ui.theme.ThemePreferenceStore
import com.example.grannysknees.watch.WatchIndexPublisher
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val cacheFile = File(filesDir, "forecast-cache.json")
        val cityFile = File(filesDir, "selected-city.json")
        val stampFile = File(filesDir, "forecast-fetch-day.txt")
        val sensitivityFile = File(filesDir, "sensitivity-profile.json")
        val feelCalibrationFile = File(filesDir, "feel-calibration.json")
        val cities = SelectedCityStore(cityFile)
        val sensitivityStore = SensitivityProfileStore(sensitivityFile)
        val feelCalibrationStore = FeelCalibrationStore(feelCalibrationFile)
        val repository = ForecastRepository(
            store = ForecastCacheStore(cacheFile),
            cityStore = cities,
            fetchStampStore = DailyFetchStampStore(stampFile),
            sensitivityStore = sensitivityStore,
            feelCalibrationStore = feelCalibrationStore,
        )
        setContent {
            GrannysKneesApp(
                repository = repository,
                cityStore = cities,
                sensitivityStore = sensitivityStore,
                feelCalibrationStore = feelCalibrationStore,
                onboardingStore = OnboardingStore(File(filesDir, "onboarding-seen")),
                themePreferenceStore = ThemePreferenceStore(
                    File(filesDir, "theme-preference.txt"),
                ),
            )
        }
        WatchIndexPublisher(this).publish(repository.presentFromCache())
    }
}

