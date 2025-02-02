package com.filantrop.pvnclient.gradle

import com.android.build.api.dsl.ApplicationExtension
import com.filantrop.pvnclient.gradle.extensions.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply(plugin = "org.jetbrains.kotlin.plugin.compose")
                apply("com.autonomousapps.dependency-analysis")
            }
            val extension = extensions.getByType<ApplicationExtension>()
            configureAndroidCompose(extension)
        }
    }
}
