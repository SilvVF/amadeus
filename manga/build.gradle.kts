plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization") version "1.8.20"
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "io.silv.manga"
    compileSdk = 34

    defaultConfig {
        minSdk = 28
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

    api(project(":core"))
    api(project(":ktor-response-mapper"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)

    implementation(libs.flow.combinetuple.kt)
    implementation(libs.tuples.kt)

    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.common)
    // optional - Paging 3 Integration
    implementation(libs.androidx.room.paging)

    //ROOM
    ksp(libs.room.ksp)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.room.coroutines)
    testImplementation(libs.room.test)

    implementation(libs.okhttp)
    //KOIN
    implementation(libs.koin.android)
    implementation(libs.koin.core)
    implementation(libs.koin.workmanager)

    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.okio)
    implementation(libs.jsoup)
    implementation(libs.kotlinx.serialization.json.okio)

    //KTOR
    implementation(libs.ktor.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.logging)
    implementation(libs.ktor.contentnegotiation)
    // Kotlin + coroutines
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.token.bucket)
    // optional - RxJava2 support
    implementation(libs.androidx.work.rxjava2)

    // optional - GCMNetworkManager support
    implementation(libs.androidx.work.gcm)

    // optional - Test helpers
    androidTestImplementation(libs.androidx.work.testing)
    implementation(libs.androidx.work.runtime.ktx)

    // optional - Multiprocess support
    implementation(libs.androidx.work.multiprocess)

    implementation(libs.androidx.paging.runtime)
    // alternatively - without Android dependencies for tests
    testImplementation(libs.androidx.paging.common)

    // KOTLIN
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlin.serialization)
    implementation(libs.kotlinx.coroutines.android)
    implementation(kotlin("reflect"))
    implementation(libs.kotlinx.coroutines.guava)
}