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
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import io.silv.amadeus.crash.CrashActivity
import io.silv.amadeus.crash.GlobalExceptionHandler
import io.silv.common.model.NetworkConnectivity
import io.silv.ui.LocalAppState
import io.silv.ui.rememberAppState
import io.silv.ui.theme.AmadeusTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val connectivity by inject<NetworkConnectivity>()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalVoyagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GlobalExceptionHandler.initialize(applicationContext, CrashActivity::class.java)

        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)

            val appState =
                rememberAppState(
                    windowSizeClass = windowSizeClass,
                    networkConnectivity = connectivity,
                    exploreSearchChannel = NavHost.globalSearchChannel,
                    bottomBarVisibilityChannel = NavHost.bottomBarVisibility
                )

            AmadeusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CompositionLocalProvider(
                        LocalAppState provides appState,
                    ) {
                        Navigator(NavHost) {
                            FadeTransition(it)
                        }
                    }
                }
            }
        }
    }
}
