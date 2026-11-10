package com.example.grannysknees.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.io.IOException

/**
 * App-private JSON for Adj sensitivity sliders and the Feel→real-feel opt-in.
 *
 * A missing or unreadable file means defaults, with Feel calibration enabled
 * and Granny's free-speech mode disabled.
 */
internal class SensitivityProfileStore(
    private val file: File,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    /** Saved Adj profile, or defaults when absent. */
    fun read(): SensitivityProfile = readState().profile

    /** Feel→real-feel opt-in; missing key defaults to **true**. */
    fun readFeelCalibrationEnabled(): Boolean = readState().feelCalibrationEnabled

    /** Whether optional salty Granny lines are allowed. */
    fun readGrannySpeaksFreely(): Boolean = readState().grannySpeaksFreely

    /** Whether the phone barometer may refine the Personal pressure signal. */
    fun readLocalPressureEnabled(): Boolean = readState().localPressureEnabled

    /** Combined Adj + Feel opt-in state. */
    fun readState(): SensitivityPreferences {
        if (!file.exists()) return SensitivityPreferences.DEFAULT
        return try {
            parse(file.readText(Charsets.UTF_8)) ?: SensitivityPreferences.DEFAULT
        } catch (_: IOException) {
            SensitivityPreferences.DEFAULT
        } catch (_: IllegalArgumentException) {
            SensitivityPreferences.DEFAULT
        } catch (_: kotlinx.serialization.SerializationException) {
            SensitivityPreferences.DEFAULT
        }
    }

    /** Replaces the saved Adj profile; keeps the current Feel opt-in. */
    fun write(profile: SensitivityProfile) {
        writeState(readState().copy(profile = profile.clamped()))
    }

    /** Updates the Feel→real-feel switch; keeps the current Adj profile. */
    fun writeFeelCalibrationEnabled(enabled: Boolean) {
        writeState(readState().copy(feelCalibrationEnabled = enabled))
    }

    /** Updates Granny's free-speech switch; keeps the other preferences. */
    fun writeGrannySpeaksFreely(enabled: Boolean) {
        writeState(readState().copy(grannySpeaksFreely = enabled))
    }

    /** Updates the local-pressure switch; keeps the other preferences. */
    fun writeLocalPressureEnabled(enabled: Boolean) {
        writeState(readState().copy(localPressureEnabled = enabled))
    }

    /** Replaces Adj and Feel opt-in together. */
    fun writeState(state: SensitivityPreferences) {
        val safe = state.copy(profile = state.profile.clamped())
        val payload = buildJsonObject {
            put("pressure", safe.profile.pressure)
            put("humidity", safe.profile.humidity)
            put("cold", safe.profile.cold)
            put("feelCalibrationEnabled", safe.feelCalibrationEnabled)
            put("grannySpeaksFreely", safe.grannySpeaksFreely)
            put("localPressureEnabled", safe.localPressureEnabled)
        }.toString()
        AtomicFileWriter.writeText(file, payload)
    }

    private fun parse(text: String): SensitivityPreferences? {
        val root = json.parseToJsonElement(text).jsonObject
        val pressure = root.float("pressure") ?: return null
        val humidity = root.float("humidity") ?: return null
        val cold = root.float("cold") ?: return null
        val feelEnabled = root["feelCalibrationEnabled"]?.jsonPrimitive?.booleanOrNull ?: true
        val grannySpeaksFreely = root["grannySpeaksFreely"]?.jsonPrimitive?.booleanOrNull ?: false
        val localPressureEnabled = root["localPressureEnabled"]?.jsonPrimitive?.booleanOrNull ?: false
        return SensitivityPreferences(
            profile = SensitivityProfile(
                pressure = pressure,
                humidity = humidity,
                cold = cold,
            ).clamped(),
            feelCalibrationEnabled = feelEnabled,
            grannySpeaksFreely = grannySpeaksFreely,
            localPressureEnabled = localPressureEnabled,
        )
    }

    private fun JsonObject.float(key: String): Float? =
        this[key]?.jsonPrimitive?.floatOrNull
            ?: this[key]?.jsonPrimitive?.contentOrNull?.toFloatOrNull()
}

/**
 * Persisted Adj sliders plus the Feel calibration opt-in.
 *
 * [feelCalibrationEnabled] defaults to true when absent from disk.
 */
internal data class SensitivityPreferences(
    val profile: SensitivityProfile = SensitivityProfile.DEFAULT,
    val feelCalibrationEnabled: Boolean = true,
    val grannySpeaksFreely: Boolean = false,
    val localPressureEnabled: Boolean = false,
) {
    companion object {
        val DEFAULT: SensitivityPreferences = SensitivityPreferences()
    }
}
