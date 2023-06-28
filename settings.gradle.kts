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
    }
}

rootProject.name = ("amadeus")
include(":app")
include(":app:ktor-response-mapper")
include(":ktor-response-mapper")
