pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = ("amadeus")
include(":app")
include(":app:ktor-response-mapper")
include(":ktor-response-mapper")
include(":core")
include(":macrobenchmark")
include(":core:network")
include(":core:common")
include(":core:database")
include(":core:data")
include(":sync")
include(":core:datastore")
include(":core:domain")
include(":feature")
include(":feature:explore")
include(":core:ui")
include(":core:navigation")
include(":feature:library")
include(":feature:reader")
include(":feature:manga")
