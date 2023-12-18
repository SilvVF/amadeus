@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    kotlin("plugin.serialization") version "1.9.20"
    id("kotlin-parcelize")
}

android {
    namespace = "io.silv.domain"
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
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation(project(":core:data"))
    implementation(project(":core:common"))

    implementation(libs.sandwich)

    implementation("androidx.compose.runtime:runtime:1.5.4")
    implementation(libs.disklrucache)
    implementation(libs.okio)
    implementation(libs.okhttp)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.work.multiprocess)
    implementation(libs.androidx.work.gcm)

    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.6")

    implementation(libs.flow.combinetuple.kt)
    implementation(libs.tuples.kt)

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // KOTLIN
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlin.serialization)
    implementation(libs.koin.workmanager)

    implementation(libs.koin.core)

    implementation(libs.androidx.paging.common)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
