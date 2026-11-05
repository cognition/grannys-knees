package com.example.grannysknees

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.grannysknees.data.ForecastCacheStore
import com.example.grannysknees.data.ForecastRepository
import com.example.grannysknees.data.SelectedCityStore
import com.example.grannysknees.ui.GrannysKneesApp
import com.example.grannysknees.ui.about.OnboardingStore
import com.example.grannysknees.watch.WatchIndexPublisher
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val cacheFile = File(filesDir, "forecast-cache.json")
        val cityFile = File(filesDir, "selected-city.json")
        val cities = SelectedCityStore(cityFile)
        val repository = ForecastRepository(
            store = ForecastCacheStore(cacheFile),
            cityStore = cities,
        )
        setContent {
            GrannysKneesApp(
                repository = repository,
                cityStore = cities,
                onboardingStore = OnboardingStore(File(filesDir, "onboarding-seen")),
            )
        }
        WatchIndexPublisher(this).publish(repository.presentFromCache())
    }
}
