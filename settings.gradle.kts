pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://jitpack.io")
    }
}



rootProject.name = ("amadeus")
include(":app")
include(":core")
include(":core:network")
include(":core:common")
include(":core:database")
include(":core:data")
include(":sync")
include(":core:datastore")
include(":feature")
include(":feature:explore")
include(":core:ui")
include(":core:navigation")
include(":feature:library")
include(":feature:reader")
include(":feature:manga")
include(":benchmark")
