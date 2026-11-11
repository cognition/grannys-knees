package com.aetherealmind.grannysknees.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.time.Instant

/** Collects a smoothed barometer sample while the app is in the foreground. */
internal class LocalBarometerSensor(
    context: Context,
    private val store: LocalPressureStore,
) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = manager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    private var smoothedPressure: Double? = null

    fun start() {
        val pressureSensor = sensor ?: return
        manager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        manager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val pressure = event.values.firstOrNull()?.toDouble() ?: return
        if (!pressure.isFinite() || pressure <= 0.0) return
        val next = smoothedPressure?.let { previous ->
            previous + ((pressure - previous) * SMOOTHING_ALPHA)
        } ?: pressure
        smoothedPressure = next
        store.write(LocalPressureReading(next, Instant.now()))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val SMOOTHING_ALPHA = 0.20
    }
}
