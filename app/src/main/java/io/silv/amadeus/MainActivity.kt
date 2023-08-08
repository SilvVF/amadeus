@file:OptIn(ExperimentalAnimationApi::class)

package io.silv.amadeus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import io.silv.amadeus.ui.screens.home.HomeTab
import io.silv.amadeus.ui.screens.library.LibraryTab
import io.silv.amadeus.ui.screens.saved.SavedTab
import io.silv.amadeus.ui.screens.search.SearchTab
import io.silv.amadeus.ui.theme.AmadeusTheme

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {

            val windowSizeClass = calculateWindowSizeClass(this)


            AmadeusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompositionLocalProvider(
                        LocalWindowSizeClass providesDefault windowSizeClass
                    ) {
                        TabNavigator(HomeTab) {
                            CurrentTab()
                        }
                    }
                }
            }
        }
    }
}

val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass?> { null }

@Composable
fun AmadeusBottomBar(
    modifier: Modifier = Modifier,
    visible: Boolean = true
) {
    AnimatedVisibility(visible = visible) {
        NavigationBar(modifier) {
            TabNavigationItem(HomeTab)
            TabNavigationItem(SearchTab)
            TabNavigationItem(SavedTab)
            TabNavigationItem(LibraryTab)
        }
    }
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

