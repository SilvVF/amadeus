plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.2")
    // KOTLIN
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlin.serialization)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.2")
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.7.2")
    implementation(project(":ktor-response-mapper"))
    implementation(libs.flow.combinetuple.kt)
    implementation(libs.tuples.kt)
}