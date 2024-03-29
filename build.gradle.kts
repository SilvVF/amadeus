// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.1" apply false
    id("com.android.library") version "8.2.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
    id("com.android.test") version "8.2.1" apply false
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
