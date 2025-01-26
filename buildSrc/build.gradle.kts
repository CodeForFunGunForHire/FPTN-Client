plugins {
    `kotlin-dsl`
}

buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "pvnclient.android.application"
            implementationClass = "com.filantrop.pvnclient.gradle.AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = "pvnclient.android.application.compose"
            implementationClass = "com.filantrop.pvnclient.gradle.AndroidApplicationComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "pvnclient.android.library.android"
            implementationClass = "com.filantrop.pvnclient.gradle.AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "pvnclient.android.library.compose"
            implementationClass = "com.filantrop.pvnclient.gradle.AndroidLibraryComposeConventionPlugin"
        }
        register("kotlinLibrary") {
            id = "pvnclient.android.library.kotlin"
            implementationClass = "com.filantrop.pvnclient.gradle.KotlinLibraryConventionPlugin"
        }
    }
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(libs.detekt)
    implementation(libs.android.gradle.plugin)
    implementation(libs.guava)
    implementation(libs.kotlin.gradle.plugin)
}
