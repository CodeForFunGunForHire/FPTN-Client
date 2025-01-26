import com.filantrop.pvnclient.gradle.extensions.ksp

plugins {
    id("pvnclient.android.library.android")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.filantrop.pvnclient.auth.data"
}

dependencies {

    implementation(project(":auth:domain"))
    implementation(project(":core:persistent"))

    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)

    ksp(libs.koin.ksp.compiler)
}
