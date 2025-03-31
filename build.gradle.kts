// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.com.android.application) apply false
    alias(libs.plugins.com.android.library) apply false
    alias(libs.plugins.org.jetbrains.kotlin.android) apply false
    alias(libs.plugins.org.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
    alias(libs.plugins.com.android.test) apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.0.3" apply false
}


subprojects {

    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    // Optionally configure plugin
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        debug.set(true)
        android.set(true)
        version.set("0.47.1")

        outputToConsole.set(true)
        outputColorName.set("RED")
        ignoreFailures.set(true)
        @Suppress("DEPRECATION")
        disabledRules.set(
            setOf(
                "function-naming",
                "final-newline",
                "trailing-comma-on-declaration-site", "trailing-comma-on-call-site")
        )

        filter {
            exclude("**/generated/**")
            include("**/kotlin/**")
        }
    }

    dependencies {
        add("ktlintRuleset", "io.nlopez.compose.rules:ktlint:0.3.7")
    }
}
