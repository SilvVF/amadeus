package io.silv.amadeus

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import io.silv.explore.ExploreTab
import io.silv.library.LibraryTab
import io.silv.manga.search.SearchTab
import io.silv.ui.ReselectTab
import io.silv.ui.locals.LocalNavBarVisibility
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


object NavHost: Screen {

    private val bottomBarVisibility = Channel<Boolean>()

    @Composable
    override fun Content() {

        val showBottomBar by produceState(initialValue = true) {
            bottomBarVisibility.receiveAsFlow().onEach { value = it }.collect()
        }

        TabNavigator(
            tab = ExploreTab,
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0),
                bottomBar = {
                    if (shouldShowBottomBar(LocalWindowSizeClass.current) && showBottomBar) {
                        AnimatedVisibility(
                            visible = showBottomBar,
                            enter = slideInVertically { it },
                            exit = slideOutVertically { -it }
                        ) {
                            AmadeusBottomBar(
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            ) { incoming ->
                CompositionLocalProvider(
                    LocalNavBarVisibility provides bottomBarVisibility
                ) {
                    Box(
                        Modifier
                            .padding(incoming)
                            .consumeWindowInsets(incoming)

                    ) {
                        CurrentTab()
                    }
                }
            }
        }
    }
}

@Composable
fun AmadeusBottomBar(
    modifier: Modifier = Modifier,
) {
    BottomAppBar(modifier) {
        TabNavigationItem(ExploreTab)
        TabNavigationItem(SearchTab)
        TabNavigationItem(LibraryTab)
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
private fun TabNavigationItem(tab: ReselectTab) {

    val tabNavigator = LocalTabNavigator.current
    val selected = tabNavigator.current == tab
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.currentOrThrow

    NavigationRailItem(
        label = { Text(tab.options.title) },
        alwaysShowLabel = false,
        selected = selected,
        onClick = {
            Log.d("Reselect", "onclick $tab")
            if (selected) {
                scope.launch {
                    Log.d("Reselect", "Sending Reselect event to $tab")
                    tab.onReselect(navigator)
                }
            } else {
                tabNavigator.current = tab
            }
        },
        icon = {
            Icon(
            painter = tab.options.icon ?: return@NavigationRailItem,
            contentDescription = tab.options.title)
        }
    )
}
