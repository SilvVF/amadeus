@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization").version(libs.versions.org.jetbrains.kotlin.android)
    id("kotlin-parcelize")
}
android {
    namespace = "io.silv.network"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}


dependencies {
    api(project(":ktor-response-mapper"))
    api(project(":core:common"))

    implementation(libs.koin.core)
    implementation(libs.okhttp)

    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.okio)
    implementation(libs.kotlinx.serialization.json.okio)

    implementation(libs.jsoup)
    //KTOR
    implementation(libs.ktor.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.logging)
    implementation(libs.ktor.contentnegotiation)

    implementation(libs.token.bucket)

    // KOTLIN
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlin.serialization)
}