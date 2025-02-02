package com.filantrop.pvnclient.gradle

import com.android.build.api.dsl.ApplicationExtension
import com.filantrop.pvnclient.gradle.extensions.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.extra

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
                apply("com.autonomousapps.dependency-analysis")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = rootProject.extra.get("targetSdkVersion") as Int
            }
            dependencies {
            }
        }
    }
}
