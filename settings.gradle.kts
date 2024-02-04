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
        maven {
            name = "Github Packages"
            url = uri("https://maven.pkg.github.com/silvvf/tokenbucket")
        }
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
include(":core:domain")
include(":feature")
include(":feature:explore")
include(":core:ui")
include(":core:navigation")
include(":feature:library")
include(":feature:reader")
include(":feature:manga")
include(":benchmark")
