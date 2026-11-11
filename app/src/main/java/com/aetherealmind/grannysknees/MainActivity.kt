package com.aetherealmind.grannysknees

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aetherealmind.grannysknees.data.DailyFetchStampStore
import com.aetherealmind.grannysknees.data.FeelCalibrationStore
import com.aetherealmind.grannysknees.data.ForecastCacheStore
import com.aetherealmind.grannysknees.data.ForecastRepository
import com.aetherealmind.grannysknees.data.LocalBarometerSensor
import com.aetherealmind.grannysknees.data.LocalPressureStore
import com.aetherealmind.grannysknees.data.SelectedCityStore
import com.aetherealmind.grannysknees.data.SensitivityProfileStore
import com.aetherealmind.grannysknees.ui.GrannysKneesApp
import com.aetherealmind.grannysknees.ui.about.OnboardingStore
import com.aetherealmind.grannysknees.ui.theme.ThemePreferenceStore
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var localBarometerSensor: LocalBarometerSensor
    private lateinit var repository: ForecastRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val cacheFile = File(filesDir, "forecast-cache.json")
        val cityFile = File(filesDir, "selected-city.json")
        val stampFile = File(filesDir, "forecast-fetch-day.txt")
        val sensitivityFile = File(filesDir, "sensitivity-profile.json")
        val feelCalibrationFile = File(filesDir, "feel-calibration.json")
        val localPressureStore = LocalPressureStore(File(filesDir, "local-pressure.json"))
        localBarometerSensor = LocalBarometerSensor(this, localPressureStore)
        val cities = SelectedCityStore(cityFile)
        val sensitivityStore = SensitivityProfileStore(sensitivityFile)
        val feelCalibrationStore = FeelCalibrationStore(feelCalibrationFile)
        repository = ForecastRepository(
            store = ForecastCacheStore(cacheFile),
            cityStore = cities,
            fetchStampStore = DailyFetchStampStore(stampFile),
            sensitivityStore = sensitivityStore,
            localPressureStore = localPressureStore,
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
    }

    override fun onStart() {
        super.onStart()
        localBarometerSensor.start()
    }

    override fun onStop() {
        localBarometerSensor.stop()
        super.onStop()
    }
}
