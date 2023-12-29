@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    kotlin("plugin.serialization") version "1.9.20"
}

android {
    namespace = "io.silv.data"
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
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    api(project(":core:network"))
    api(project(":core:database"))
    api(project(":core:common"))
    api(project(":core:datastore"))
    api(project(":core:domain"))

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.disklrucache)
    implementation(libs.image.decoder)
    implementation(libs.unifile)
    implementation(libs.sandwich.ktor)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.guava)

    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlin.serialization)
    implementation(libs.kotlinx.serialization.json.okio)
    implementation(libs.kotlin.collections.immutable)

    // KOIN
    implementation(libs.koin.android)
    implementation(libs.koin.core)
    implementation(libs.koin.workmanager)

    // Paging 3
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.common)

    // ROOM
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.room.coroutines)
    testImplementation(libs.room.test)

    // WorkManager
    androidTestImplementation(libs.androidx.work.testing)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.work.multiprocess)
    implementation(libs.androidx.work.gcm)
}
