// Top-level build file where you can add configuration options common to all sub-projects/modules.
import com.filantrop.pvnclient.gradle.options.DetektOptions.applyDetektOptions
import com.filantrop.pvnclient.gradle.options.FormattingOptions.applyPrecheckOptions

buildscript {

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

repositories {
    mavenCentral()
}

extra["compileSdkVersion"] = 35
extra["minSdkVersion"] = 28
extra["targetSdkVersion"] = 35
extra["versionMajor"] = 1
extra["versionMinor"] = 0
extra["versionPatch"] = 0
extra["versionBuild"] = 6

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

plugins {
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.deps.sorting) apply true
    alias(libs.plugins.deps.unused) apply true
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

applyPrecheckOptions()
applyDetektOptions()

subprojects {
    apply(plugin = "com.squareup.sort-dependencies")
}

dependencyAnalysis {
    val fail = "fail"
    val ignore = "ignore"
    issues {
        all {
            onUnusedDependencies { severity(fail) }
            onUsedTransitiveDependencies { severity(ignore) }
            onIncorrectConfiguration { severity(ignore) }
            onCompileOnly { severity(ignore) }
            onRuntimeOnly { severity(ignore) }
            onUnusedAnnotationProcessors { severity(ignore) }
            onRedundantPlugins { severity(ignore) }
        }
    }
}
