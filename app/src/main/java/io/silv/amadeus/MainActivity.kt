@file:OptIn(ExperimentalAnimationApi::class)

package io.silv.amadeus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import io.silv.amadeus.ui.screens.home.HomeTab
import io.silv.amadeus.ui.screens.library.LibraryTab
import io.silv.amadeus.ui.screens.saved.SavedTab
import io.silv.amadeus.ui.theme.AmadeusTheme
import io.silv.amadeus.ui.theme.LocalBottomBarVisibility
import io.silv.amadeus.ui.theme.LocalPaddingValues

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AmadeusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TabNavigator(HomeTab) {
                        Scaffold(
                            contentWindowInsets = WindowInsets(0,0,0,0),
                            bottomBar = {
                                val visible by LocalBottomBarVisibility.current
                                if (visible) {
                                    NavigationBar {
                                        TabNavigationItem(HomeTab)
                                        TabNavigationItem(SavedTab)
                                        TabNavigationItem(LibraryTab)
                                    }
                                }
                            },
                            content = { paddingValues ->
                                var localPadding by LocalPaddingValues.current
                                localPadding = paddingValues
                                CurrentTab()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabNavigationItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current

    NavigationBarItem(
        label = { Text(tab.options.title) },
        selected = tabNavigator.current == tab,
        onClick = { tabNavigator.current = tab },
        icon = { Icon(
            painter = tab.options.icon ?: return@NavigationBarItem,
            contentDescription = tab.options.title)
        }
    )
}

