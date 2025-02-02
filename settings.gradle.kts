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
include(":core:designsystem")
include(":core:model")
include(":core:network")
include(":core:persistent")
include(":home:data")
include(":home:domain")
include(":home:ui")
include(":settings:data")
include(":settings:domain")
include(":settings:ui")
include(":vpnclient")
