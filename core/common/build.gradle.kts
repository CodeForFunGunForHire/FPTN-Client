import com.filantrop.pvnclient.gradle.extensions.ksp

plugins {
    id("pvnclient.android.library.kotlin")
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)

    ksp(libs.koin.ksp.compiler)
}
