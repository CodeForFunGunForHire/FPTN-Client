import com.filantrop.pvnclient.gradle.extensions.ksp

plugins {
    id("pvnclient.android.application")
    id("pvnclient.android.application.compose")
    id("kotlin-kapt")
    alias(libs.plugins.ksp)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.filantrop.pvnclient"
    compileSdk = rootProject.extra.get("compileSdkVersion") as Int

    defaultConfig {
        applicationId = "com.filantrop.pvnclient"
        val versionMajor: Int by rootProject.extra
        val versionMinor: Int by rootProject.extra
        val versionPatch: Int by rootProject.extra
        val versionBuild: Int by rootProject.extra
        versionCode = 1000 * (1000 * versionMajor + 100 * versionMinor + versionPatch) + versionBuild
        versionName = "$versionMajor.$versionMinor.$versionPatch.$versionBuild"

        minSdk = rootProject.extra.get("minSdkVersion") as Int
        targetSdk = rootProject.extra.get("targetSdkVersion") as Int

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
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
        viewBinding = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":auth:domain"))
    implementation(project(":auth:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:persistent"))
    implementation(project(":vpnclient"))
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.runtime.android)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.room.runtime)
    implementation(libs.guava)
    implementation(libs.ipaddress)
    implementation(libs.koin.android)
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.material)
    implementation(libs.okhttp)
    implementation(libs.protobuf.javalite)

    compileOnly(libs.lombock)

    annotationProcessor(libs.androidx.room.compiler)
    annotationProcessor(libs.lombock)

    testImplementation(libs.junit)
    testImplementation(libs.koin.test.jvm)

    ksp(libs.koin.ksp.compiler)
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

protobuf {
    protoc {
        artifact = libs.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach {
            it.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}
