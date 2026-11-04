package com.example.grannysknees

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.grannysknees.data.ForecastCacheStore
import com.example.grannysknees.data.ForecastRepository
import com.example.grannysknees.ui.GrannysKneesApp
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val cacheFile = File(filesDir, "forecast-cache.json")
        val repository = ForecastRepository(store = ForecastCacheStore(cacheFile))
        setContent {
            GrannysKneesApp(repository = repository)
        }
    }
}
