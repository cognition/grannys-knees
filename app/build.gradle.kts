import org.gradle.api.GradleException

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

/**
 * Release upload-key credentials from [keystore.properties] at the repo root and/or
 * environment variables. Property names match the standard Android keystore file.
 *
 * The properties file is parsed as plain `key=value` lines (not
 * `java.util.Properties`) so passwords may contain `\`, `:`, and `#` without
 * Java escape mangling.
 */
data class ReleaseSigningCredentials(
    val storeFile: java.io.File,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

private object ReleaseSigningCredentialsLoader {
    private const val STORE_FILE = "storeFile"
    private const val STORE_PASSWORD = "storePassword"
    private const val KEY_ALIAS = "keyAlias"
    private const val KEY_PASSWORD = "keyPassword"

    private const val ENV_STORE_FILE = "RELEASE_STORE_FILE"
    private const val ENV_STORE_PASSWORD = "RELEASE_STORE_PASSWORD"
    private const val ENV_KEY_ALIAS = "RELEASE_KEY_ALIAS"
    private const val ENV_KEY_PASSWORD = "RELEASE_KEY_PASSWORD"

    fun load(rootDir: java.io.File): ReleaseSigningCredentials? {
        val fromFile = loadPlainProperties(rootDir.resolve("keystore.properties"))

        fun resolve(name: String, envName: String): String? =
            System.getenv(envName)?.trim()?.takeIf { it.isNotEmpty() }
                ?: fromFile[name]?.trim()?.takeIf { it.isNotEmpty() }

        val storeFilePath = resolve(STORE_FILE, ENV_STORE_FILE) ?: return null
        val storePassword = resolve(STORE_PASSWORD, ENV_STORE_PASSWORD) ?: return null
        val keyAlias = resolve(KEY_ALIAS, ENV_KEY_ALIAS) ?: return null
        val keyPassword = resolve(KEY_PASSWORD, ENV_KEY_PASSWORD) ?: return null

        val storeFile = rootDir.resolve(storeFilePath)
        if (!storeFile.isFile) {
            return null
        }

        return ReleaseSigningCredentials(
            storeFile = storeFile,
            storePassword = storePassword,
            keyAlias = keyAlias,
            keyPassword = keyPassword,
        )
    }

    /**
     * Reads `key=value` lines without Java Properties backslash escaping.
     */
    private fun loadPlainProperties(file: java.io.File): Map<String, String> {
        if (!file.isFile) {
            return emptyMap()
        }
        val values = linkedMapOf<String, String>()
        file.readLines(Charsets.UTF_8).forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                return@forEach
            }
            val separator = line.indexOf('=')
            if (separator <= 0) {
                return@forEach
            }
            val key = line.substring(0, separator).trim()
            val value = line.substring(separator + 1).trim()
            if (key.isNotEmpty()) {
                values[key] = value
            }
        }
        return values
    }
}

val releaseSigningCredentials = ReleaseSigningCredentialsLoader.load(rootProject.projectDir)
val uploadSigningReady = releaseSigningCredentials != null
val allowDebugReleaseSigning =
    System.getenv("ALLOW_DEBUG_RELEASE_SIGNING").equals("true", ignoreCase = true)

android {
    namespace = "ca.aeo3.grannysknees"
    compileSdk = 37

    defaultConfig {
        applicationId = "ca.aeo3.grannysknees"
        minSdk = 26
        targetSdk = 37
        versionCode = 150
        versionName = "0.3.82.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (uploadSigningReady) {
            create("release") {
                val creds = releaseSigningCredentials!!
                storeFile = creds.storeFile
                storePassword = creds.storePassword
                keyAlias = creds.keyAlias
                keyPassword = creds.keyPassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig =
                when {
                    uploadSigningReady -> signingConfigs.getByName("release")
                    allowDebugReleaseSigning -> signingConfigs.getByName("debug")
                    else -> null
                }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        // AtomicFile.finishWrite touches android.util.Log; return defaults under JVM unit tests.
        unitTests.isReturnDefaultValues = true
    }
}

tasks.matching { it.name == "bundleRelease" || it.name == "assembleRelease" }.configureEach {
    val signingReady = uploadSigningReady
    val debugFallbackAllowed = allowDebugReleaseSigning
    doFirst {
        if (signingReady) {
            return@doFirst
        }
        if (debugFallbackAllowed) {
            this.logger.warn(
                """
                |
                |**********************************************************************
                | Release build is signed with the DEBUG key — NOT uploadable to Play.
                | ALLOW_DEBUG_RELEASE_SIGNING=true is set for local inspection only.
                | Configure upload signing via keystore.properties or RELEASE_* env vars.
                | See docs/release/google-play/UPLOAD-SIGNING.md
                |**********************************************************************
                |
                """
                    .trimMargin(),
            )
            return@doFirst
        }
        throw GradleException(
            "${this.name}: release signing credentials are missing. " +
                "Provide keystore.properties (storeFile, storePassword, keyAlias, keyPassword) " +
                "or RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, " +
                "RELEASE_KEY_PASSWORD. For a local debug-signed release artefact only, set " +
                "ALLOW_DEBUG_RELEASE_SIGNING=true. See docs/release/google-play/UPLOAD-SIGNING.md",
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.glance.appwidget)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)

    // T03: Open-Meteo client JSON parsing (runtime JSON element API; no serialization plugin).
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit4)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
}
