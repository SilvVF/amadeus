plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization") version "1.8.20"
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "io.silv.amadeus"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.silv.amadeus"
        minSdk = 28
        targetSdk = 34
        versionCode = 3
        versionName = "3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        val release = getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // In real app, this would use its own release keystore
            signingConfig = signingConfigs.getByName("debug")
        }

        create("benchmark") {
            initWith(release)
            signingConfig = signingConfigs.getByName("debug")
            // [START_EXCLUDE silent]
            // Selects release buildType if the benchmark buildType not available in other modules.
            matchingFallbacks.add("release")
            // [END_EXCLUDE]
            proguardFiles("benchmark-rules.pro")
        }
        getByName("debug") {
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.6"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
dependencies {

    implementation(project(":ktor-response-mapper"))
    implementation(project(":manga"))
    implementation(project(":core"))

    implementation("com.google.accompanist:accompanist-webview:0.30.1")

    implementation(libs.flow.combinetuple.kt)
    implementation(libs.tuples.kt)

    implementation(libs.androidx.material3.window.size)

    // COMPOSE
    val composeBom = platform(libs.androidx.compose.bom)
    implementation("androidx.compose.ui:ui-util")
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material)
    androidTestImplementation(libs.androidx.compose.ui.test.junit)
    androidTestImplementation(composeBom)
    debugImplementation(libs.androidx.compose.ui.manifest)
    implementation(libs.androidx.activity.compose)
    implementation(libs.orbital)
    implementation("androidx.compose.ui:ui-graphics")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.common)
    // optional - Paging 3 Integration
    implementation(libs.androidx.room.paging)

    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-graphics")

    //Datastore
    implementation(libs.androidx.datastore.preferences)


    implementation(libs.androidx.navigation.compose)

    //ROOM
    ksp(libs.room.ksp)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.room.coroutines)
    testImplementation(libs.room.test)

    //VOYAGER
    implementation(libs.voyager.androidx)
    implementation(libs.voyager.koin)
    implementation(libs.voyager.navigator)
    implementation(libs.voyager.transitions)
    implementation(libs.voyager.tabNavigator)

    //KOIN
    implementation(libs.koin.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.core)
    implementation(libs.koin.navigation)
    implementation(libs.koin.workmanager)

    // COIL
    implementation(libs.coil.compose)
    implementation(libs.coil)
    implementation(libs.coil.svg)

    // KOTLIN
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlin.serialization)
    implementation(kotlin("reflect"))
    implementation(libs.kotlinx.coroutines.guava)
}