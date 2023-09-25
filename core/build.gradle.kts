plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlin.serialization)
    implementation(libs.kotlinx.coroutines.android)
    implementation(project(":ktor-response-mapper"))
    implementation(libs.flow.combinetuple.kt)
    implementation(libs.tuples.kt)
}