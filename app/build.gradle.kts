plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    alias(libs.plugins.compose.compiler)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

android {
    namespace = "io.silv.amadeus"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.silv.amadeus"
        minSdk = 28
        targetSdk = 35
        versionCode = 8
        versionName = "8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }

        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }

    }
    compileOptions {
        // For AGP 4.1+
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    composeCompiler {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
        stabilityConfigurationFiles.set(
            listOf(
                rootProject.layout.projectDirectory.file("stability_config.conf")
            )
        )

    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:navigation"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
    implementation(project(":core:ui"))

    implementation(project(":sync"))

    implementation(project(":feature:explore"))
    implementation(project(":feature:library"))
    implementation(project(":feature:reader"))
    implementation(project(":feature:manga"))

    implementation(libs.androidx.material3.adaptive.navigation.suite)
    // COMPOSE
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.bundles.compose.ui)
    androidTestImplementation(libs.androidx.compose.ui.test.junit)
    androidTestImplementation(composeBom)
    debugImplementation(libs.androidx.compose.ui.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.font.awesome)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    // Datastore
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okio)
    implementation(libs.bundles.voyager)

    // ROOM
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.room.coroutines)
    testImplementation(libs.room.test)


    implementation(libs.bundles.coil)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlin.serialization)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.ktor.core)

    implementation(libs.kotlinx.io.core)
    implementation(libs.kotlinx.io.okio)

    implementation(libs.unifile)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.ktor)

    implementation(libs.molecule.runtime)
}
