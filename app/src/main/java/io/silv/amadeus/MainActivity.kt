package io.silv.amadeus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import io.silv.amadeus.crash.CrashActivity
import io.silv.amadeus.crash.GlobalExceptionHandler
import io.silv.amadeus.dependency.rememberDataDependency
import io.silv.common.model.AppTheme
import io.silv.explore.ExploreTab
import io.silv.ui.LocalAppState
import io.silv.ui.rememberAppState
import io.silv.ui.theme.AmadeusTheme
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalVoyagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GlobalExceptionHandler.initialize(applicationContext, CrashActivity::class.java)

        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val settingsStore = rememberDataDependency { settingsStore }
            val connectivity = rememberDataDependency { connectivity }

            val appState =
                rememberAppState(
                    windowSizeClass = windowSizeClass,
                    networkConnectivity = connectivity,
                    searchTab = ExploreTab,
                    baseScreen = NavHost
                )

            val theme by produceState(initialValue = AppTheme.DYNAMIC_COLOR_DEFAULT) {
                settingsStore.observe().onEach { value = it.theme }.launchIn(this)
            }

            AmadeusTheme(theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CompositionLocalProvider(
                        LocalAppState provides appState,
                    ) {
                        Navigator(NavHost) {
                            DisposableEffect(Unit) {
                                appState.navigator = it
                                onDispose { appState.navigator = null }
                            }

                            FadeTransition(it)
                        }
                    }
                }
            }
        }
    }
}