rootProject.name = "PVNClient"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

include(":app")
include(":auth:data")
include(":auth:domain")
include(":auth:ui")
include(":core:common")
include(":core:model")
include(":core:network")
include(":core:persistent")
include(":vpnclient")
