import com.filantrop.pvnclient.gradle.extensions.ksp

plugins {
    id("pvnclient.android.library.android")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.filantrop.pvnclient.settings.data"
}

dependencies {

    ksp(libs.koin.ksp.compiler)
}
