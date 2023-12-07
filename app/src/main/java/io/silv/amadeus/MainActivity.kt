@file:OptIn(ExperimentalAnimationApi::class)

package io.silv.amadeus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.navigator.Navigator
import io.silv.amadeus.crash.CrashActivity
import io.silv.amadeus.crash.GlobalExceptionHandler
import io.silv.ui.theme.AmadeusTheme


class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalVoyagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GlobalExceptionHandler.initialize(applicationContext, CrashActivity::class.java)

        enableEdgeToEdge()

        setContent {

            val windowSizeClass = calculateWindowSizeClass(this)

            AmadeusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CompositionLocalProvider(
                        LocalWindowSizeClass provides windowSizeClass
                    ) {
                        Navigator(NavHost)
                    }
                }
            }
        }
    }
}


fun shouldShowBottomBar(windowSizeClass: WindowSizeClass?): Boolean {
    return (windowSizeClass?.widthSizeClass ?: return true) == WindowWidthSizeClass.Compact
}

val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass> { error("Window size class was not provided") }

