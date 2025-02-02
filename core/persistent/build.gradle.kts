import com.filantrop.pvnclient.gradle.extensions.ksp

plugins {
    id("pvnclient.android.library.android")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.filantrop.pvnclient.core.persistent"
}

dependencies {

    implementation(libs.datastore)
    implementation(libs.koin.core)

    ksp(libs.koin.ksp.compiler)
}
