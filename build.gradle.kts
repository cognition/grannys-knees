// Force KGP/KSP above AGP 9's bundled 2.2.10 baseline (CVE-2026-53914).
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
        classpath(
            "com.google.devtools.ksp:symbol-processing-gradle-plugin:${libs.versions.ksp.get()}",
        )
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
}
