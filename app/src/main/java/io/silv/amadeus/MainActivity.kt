@file:OptIn(ExperimentalAnimationApi::class)

package io.silv.amadeus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import io.silv.amadeus.ui.screens.home.HomeTab
import io.silv.amadeus.ui.screens.home.shouldShowBottomBar
import io.silv.amadeus.ui.screens.library.LibraryTab
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
                        LocalWindowSizeClass providesDefault windowSizeClass,
                        LocalBottomBarVisibility providesDefault rememberSaveable {
                            mutableStateOf(true)
                        }
                    ) {
                        val showBar by LocalBottomBarVisibility.current

                        TabNavigator(
                            tab = HomeTab,
                            disposeNestedNavigators = false,
                        ) {
                            Scaffold(
                                bottomBar = {
                                    if (showBar) {
                                        AmadeusBottomBar()
                                    }
                                }
                            ) { incoming ->
                                Box(
                                    Modifier
                                        .padding(
                                            bottom = (incoming.calculateBottomPadding() - 16.dp).coerceAtLeast(0.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmadeusScaffold(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state = rememberTopAppBarState()),
    showBottomBar: Boolean = true,
    topBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    content: @Composable (PaddingValues) -> Unit
) {
    val windowSizeClass = LocalWindowSizeClass.current
    var bottomBarVisibility by LocalBottomBarVisibility.current

    LaunchedEffect(showBottomBar) {
        bottomBarVisibility = showBottomBar
    }

    if (shouldShowBottomBar(windowSizeClass)) {
        Scaffold(
            topBar = {
                topBar()
            },
            snackbarHost = snackbarHost,
            contentColor = contentColor,
            contentWindowInsets = contentWindowInsets,
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            content(it)
        }
    } else {
        Row {
            if (showBottomBar) {
                AmadeusNavRail(visible = showBottomBar)
            }
            Scaffold(
                topBar = {
                    topBar()
                },
                snackbarHost = snackbarHost,
                contentColor = contentColor,
                contentWindowInsets = contentWindowInsets,
                modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                content(it)
            }
        }
    }
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
            TabNavigationItem(HomeTab)
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
            TabNavigationItem(HomeTab)
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

