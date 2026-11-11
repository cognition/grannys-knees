package com.aetherealmind.grannysknees.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import com.aetherealmind.grannysknees.data.PlaceSource
import com.aetherealmind.grannysknees.data.SelectedCity
import java.io.IOException
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Optional foreground fix. A missing permission or a missing fix returns null
 * so the city search stays available. No background tracking.
 */
internal class OptionalGpsPlaceSource(
    context: Context,
) {
    private val appContext: Context = context.applicationContext
    private val locationManager: LocationManager? =
        appContext.getSystemService(LocationManager::class.java)

    /** True when fine or coarse location has already been granted. */
    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /** A city from the current fix, or null when GPS cannot answer. */
    fun currentCity(): SelectedCity? {
        val manager = locationManager
        if (manager == null || !hasPermission()) return null
        val location = awaitGps(manager) ?: return null
        return cityFrom(location)
    }

    @SuppressLint("MissingPermission")
    private fun awaitGps(manager: LocationManager): Location? {
        if (!providerEnabled(manager, LocationManager.GPS_PROVIDER)) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            awaitCurrent(manager)
        } else {
            awaitLegacy(manager)
        }
    }

    private fun awaitCurrent(manager: LocationManager): Location? {
        val latch = CountDownLatch(1)
        val found = AtomicReference<Location>()
        val signal = CancellationSignal()
        try {
            manager.getCurrentLocation(
                LocationManager.GPS_PROVIDER,
                signal,
                appContext.mainExecutor,
            ) { location ->
                if (location != null) found.set(location)
                latch.countDown()
            }
        } catch (_: SecurityException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (!latch.await(GPS_WAIT_SECONDS, TimeUnit.SECONDS)) {
            signal.cancel()
        }
        return found.get()
    }

    @SuppressLint("MissingPermission")
    private fun awaitLegacy(manager: LocationManager): Location? {
        val latch = CountDownLatch(1)
        val found = AtomicReference<Location>()
        val listener = LocationListener { location ->
            found.set(location)
            latch.countDown()
        }
        try {
            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0f,
                listener,
                Looper.getMainLooper(),
            )
        } catch (_: SecurityException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        latch.await(GPS_WAIT_SECONDS, TimeUnit.SECONDS)
        manager.removeUpdates(listener)
        return found.get()
    }

    private fun providerEnabled(manager: LocationManager, provider: String): Boolean =
        try {
            manager.isProviderEnabled(provider)
        } catch (_: IllegalArgumentException) {
            false
        }

    private fun cityFrom(location: Location): SelectedCity {
        val zone = ZoneId.systemDefault().id
        val named = placeName(location.latitude, location.longitude)
        if (named != null) {
            return SelectedCity(
                label = named.label,
                latitude = location.latitude,
                longitude = location.longitude,
                timezone = zone,
                admin1 = named.admin1,
                countryCode = named.countryCode,
                placeSource = PlaceSource.Gps,
            )
        }
        return SelectedCity(
            label = String.format(Locale.CANADA, "%.2f, %.2f", location.latitude, location.longitude),
            latitude = location.latitude,
            longitude = location.longitude,
            timezone = zone,
            admin1 = "",
            countryCode = "",
            placeSource = PlaceSource.Gps,
        )
    }

    private fun placeName(latitude: Double, longitude: Double): NamedPlace? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(appContext, Locale.CANADA)
        val addresses = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                awaitGeocode(geocoder, latitude, longitude)
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)
            }
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
        val address = addresses?.firstOrNull() ?: return null
        val city = address.locality?.trim().orEmpty()
        val region = address.adminArea?.trim().orEmpty()
        val label = when {
            city.isNotEmpty() && region.isNotEmpty() -> "$city, $region"
            city.isNotEmpty() -> city
            region.isNotEmpty() -> region
            else -> return null
        }
        return NamedPlace(
            label = label,
            admin1 = region,
            countryCode = address.countryCode?.trim().orEmpty(),
        )
    }

    private fun awaitGeocode(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double,
    ): List<android.location.Address> {
        val latch = CountDownLatch(1)
        val found = AtomicReference<List<android.location.Address>>(emptyList())
        geocoder.getFromLocation(
            latitude,
            longitude,
            1,
            object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<android.location.Address>) {
                    found.set(addresses)
                    latch.countDown()
                }

                override fun onError(errorMessage: String?) {
                    latch.countDown()
                }
            },
        )
        latch.await(GEOCODE_WAIT_SECONDS, TimeUnit.SECONDS)
        return found.get()
    }

    private data class NamedPlace(
        val label: String,
        val admin1: String,
        val countryCode: String,
    )

    private companion object {
        const val GPS_WAIT_SECONDS: Long = 8L
        const val GEOCODE_WAIT_SECONDS: Long = 4L
    }
}
