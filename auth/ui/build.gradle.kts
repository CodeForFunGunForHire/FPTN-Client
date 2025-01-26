import com.filantrop.pvnclient.gradle.extensions.ksp

plugins {
    id("pvnclient.android.library.android")
    id("pvnclient.android.library.compose")
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.filantrop.pvnclient.auth.ui"
}

dependencies {

    implementation(project(":auth:data"))
    implementation(project(":auth:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.runtime.android)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.koin.android)
    implementation(libs.koin.core)

    ksp(libs.koin.ksp.compiler)
}
