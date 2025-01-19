rootProject.name = "PVNClient"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

include(":app")
include(":core:common")
include(":core:model")
include(":core:persistent")
include(":vpnclient")
