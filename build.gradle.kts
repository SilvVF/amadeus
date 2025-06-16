import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.com.android.application) apply false
    alias(libs.plugins.com.android.library) apply false
    alias(libs.plugins.org.jetbrains.kotlin.android) apply false
    alias(libs.plugins.org.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.com.android.test) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.android") {
        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                freeCompilerArgs.addAll(
                    "-opt-in=androidx.compose.animation.ExperimentalSharedTransitionApi",
                    "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
                    "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
                    "-opt-in=androidx.compose.material.ExperimentalMaterialApi",
                    "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                    "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
                    "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
                    "-opt-in=androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
                    "-opt-in=coil.annotation.ExperimentalCoilApi",
                    "-opt-in=kotlinx.coroutines.FlowPreview",
                )
            }
        }
    }
    plugins.withId("com.android.library") {
//        extensions.configure<LibraryExtension> {
//            compileOptions {
//                isCoreLibraryDesugaringEnabled = true
//            }
//        }
//
//        dependencies {
//            add("coreLibraryDesugaring", libs.desugar.jdk.libs)
//        }
    }

    plugins.withId("com.android.application") {
//        extensions.configure<AppExtension> {
//            compileOptions {
//                isCoreLibraryDesugaringEnabled = true
//            }
//        }
//        dependencies {
//            add("coreLibraryDesugaring", libs.desugar.jdk.libs)
//        }
    }
}