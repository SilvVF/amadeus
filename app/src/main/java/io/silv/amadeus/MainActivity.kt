@file:OptIn(ExperimentalAnimationApi::class)

package io.silv.amadeus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import io.silv.amadeus.crash.CrashActivity
import io.silv.amadeus.crash.GlobalExceptionHandler
import io.silv.explore.ExploreTab
import io.silv.library.LibraryTab
import io.silv.manga.search.SearchTab
import io.silv.ui.theme.AmadeusTheme


class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GlobalExceptionHandler.initialize(applicationContext, CrashActivity::class.java)

        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)

            AmadeusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompositionLocalProvider(
                        LocalWindowSizeClass providesDefault windowSizeClass,
                        LocalBottomBarVisibility providesDefault rememberSaveable {
                            mutableStateOf(true)
                        }
                    ) {
                        val showBar by LocalBottomBarVisibility.current

                        TabNavigator(tab = ExploreTab, disposeNestedNavigators = true) {
                            Scaffold(
                                bottomBar = {
                                    if (shouldShowBottomBar(windowSizeClass) && showBar) {
                                        AmadeusBottomBar()
                                    }
                                }
                            ) { incoming ->
                                Box(
                                    Modifier
                                        .padding(
                                            bottom = (incoming.calculateBottomPadding() - 16.dp).coerceAtLeast(
                                                0.dp
                                            )
                                        )
                                        .fillMaxSize()
                                ) {
                                    CurrentTab()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


fun shouldShowBottomBar(windowSizeClass: WindowSizeClass?): Boolean {
    return (windowSizeClass?.widthSizeClass ?: return true) == WindowWidthSizeClass.Compact
}

val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass?> { null }
val LocalBottomBarVisibility = compositionLocalOf {
    mutableStateOf(true)
}
@Composable
fun AmadeusBottomBar(
    modifier: Modifier = Modifier,
    visible: Boolean = true
) {
    AnimatedVisibility(visible = visible) {
        NavigationBar(modifier) {
            TabNavigationItem(ExploreTab)
            TabNavigationItem(SearchTab)
            TabNavigationItem(LibraryTab)
        }
    }
}

@Composable
fun AmadeusNavRail(
    modifier: Modifier = Modifier,
    visible: Boolean = true
) {
    AnimatedVisibility(visible = visible) {
        NavigationRail(modifier) {
            TabNavigationItem(ExploreTab)
            TabNavigationItem(SearchTab)
            TabNavigationItem(LibraryTab)
        }
    }
}

@Composable
private fun ColumnScope.TabNavigationItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current

    NavigationRailItem(
        label = { Text(tab.options.title) },
        alwaysShowLabel = false,
        selected = tabNavigator.current == tab,
        onClick = { tabNavigator.current = tab },
        icon = { Icon(
            painter = tab.options.icon ?: return@NavigationRailItem,
            contentDescription = tab.options.title)
        }
    )
}

@Composable
private fun RowScope.TabNavigationItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current

    NavigationBarItem(
        label = { Text(tab.options.title) },
        alwaysShowLabel = false,
        selected = tabNavigator.current == tab,
        onClick = { tabNavigator.current = tab },
        icon = { Icon(
            painter = tab.options.icon ?: return@NavigationBarItem,
            contentDescription = tab.options.title)
        }
    )
}

