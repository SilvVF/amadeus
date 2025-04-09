plugins {
    id("java-library")
    alias(libs.plugins.org.jetbrains.kotlin.jvm)
    kotlin("plugin.serialization")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlin.serialization)
    compileOnly(libs.compose.stable.marker)
}
