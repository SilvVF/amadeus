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
include(":manga")
include(":core")
