plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
}

android {
    namespace = "io.silv.reader"
    compileSdk = 35

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    composeCompiler {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
         stabilityConfigurationFiles.add(
            rootProject.layout.projectDirectory.file("stability_config.conf")
        )
    }
}

dependencies {
    api(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
    implementation(project(":core:data"))
    implementation(project(":sync"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)

    implementation(libs.sandwich)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)

    // COMPOSE
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.bundles.compose.ui)
    androidTestImplementation(libs.androidx.compose.ui.test.junit)
    androidTestImplementation(composeBom)
    debugImplementation(libs.androidx.compose.ui.manifest)

    implementation(libs.swipe)
    implementation(libs.zoomable.image.coil)

    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlin.serialization)
    implementation(libs.kotlinx.serialization.json.okio)
    implementation(libs.sandwich.ktor)

    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.common)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlin.serialization)

    implementation(libs.bundles.voyager)

    

    implementation(libs.bundles.coil)
    debugImplementation(libs.androidx.ui.tooling)
}
