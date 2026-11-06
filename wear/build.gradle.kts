plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.grannysknees.wear"
    compileSdk = 37

    defaultConfig {
        // Separate from the phone id, which stays com.example.grannysknees.
        applicationId = "com.example.grannysknees.wear"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.3.6.0"
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
