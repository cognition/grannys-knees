plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "ca.aeo3.grannysknees.wear"
    compileSdk = 37

    defaultConfig {
        // Separate from the phone id, which stays ca.aeo3.grannysknees.
        applicationId = "ca.aeo3.grannysknees.wear"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "0.3.82.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.wear.tiles:tiles:1.5.0")
    implementation("com.google.guava:guava:33.4.0-android")
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
}
