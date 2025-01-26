package com.filantrop.pvnclient.gradle.options

import org.gradle.api.Project
import org.gradle.api.attributes.Bundling
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin

object FormattingOptions {
    fun Project.applyPrecheckOptions() {
        val ktlint by configurations.creating

        dependencies {
            ktlint("com.pinterest.ktlint:ktlint-cli:1.5.0") {
                attributes {
                    attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
                }
            }
            // ktlint(project(":custom-ktlint-ruleset")) // in case of custom ruleset
        }

        tasks.register<JavaExec>("ktlintCheck") {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Check Kotlin code style"
            classpath = ktlint
            mainClass.set("com.pinterest.ktlint.Main")
            // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
            args(
                "**/src/**/*.kt",
                "**.kts",
                "!**/build/**",
            )
        }

        tasks.register<JavaExec>("ktlintFormat") {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Check Kotlin code style and format"
            classpath = ktlint
            mainClass.set("com.pinterest.ktlint.Main")
            jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
            // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
            args(
                "-F",
                "**/src/**/*.kt",
                "**.kts",
                "!**/build/**",
            )
        }
    }
}
