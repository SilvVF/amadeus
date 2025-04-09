package io.silv.amadeus

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import io.silv.amadeus.dependency.rememberDataDependency
import io.silv.explore.ExploreTab
import io.silv.library.LibraryTab
import io.silv.manga.history.RecentsTab
import io.silv.manga.settings.MoreTab
import io.silv.ui.LocalAppState
import io.silv.ui.ReselectTab

object NavHost : Screen {

    private fun readResolve(): Any = this

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedContentLambdaTargetStateParameter")
    @Composable
    override fun Content() {
        val appState = LocalAppState.current
        val nav = LocalNavigator.current

        TabNavigator(
            tab = LibraryTab,
        ) { tabNavigator ->
            DisposableEffect(Unit) {
                appState.tabNavigator = tabNavigator
                onDispose { appState.tabNavigator = null }
            }

            CompositionLocalProvider(LocalNavigator provides nav) {
                NavigationSuiteScaffold(
                    navigationSuiteItems = {
                        listOf(LibraryTab, RecentsTab, ExploreTab, MoreTab)
                            .fastForEach {
                                item(
                                    modifier = Modifier,
                                    selected = tabNavigator.current == it,
                                    onClick = { appState.onTabSelected(it) },
                                    label = { Text(it.options.title) },
                                    icon = {
                                        Icon(
                                            painter = it.options.icon ?: return@item,
                                            contentDescription = it.options.title,
                                        )
                                    }
                                )
                            }
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AnimatedContent(
                        modifier = Modifier.fillMaxSize(),
                        targetState = tabNavigator.current,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "tabContent",
                    ) {
                        tabNavigator.saveableState(key = "currentTab", it) {
                            it.Content()
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AmadeusBottomBar(modifier: Modifier = Modifier) {

    val getUpdateCount = rememberDataDependency { getUpdateCount }

    val mangaWithUpdates by produceState(initialValue = 0) {
        getUpdateCount.subscribe().collect { value = it }
    }

    BottomAppBar(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabNavigationItemWithNotifications(LibraryTab, mangaWithUpdates)
            TabNavigationItem(RecentsTab)
            TabNavigationItem(ExploreTab)
            TabNavigationItem(MoreTab)
        }
    }
}

@Composable
fun AmadeusNavRail(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    val getUpdateCount = rememberDataDependency { getUpdateCount }

    val mangaWithUpdates by produceState(initialValue = 0) {
        getUpdateCount.subscribe().collect { value = it }
    }

    AnimatedVisibility(visible) {
        NavigationRail(modifier) {
            TabNavigationItemWithNotifications(LibraryTab, mangaWithUpdates)
            TabNavigationItem(RecentsTab)
            TabNavigationItem(ExploreTab)
            TabNavigationItem(MoreTab)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabNavigationItemWithNotifications(tab: ReselectTab, notificationCount: Int) {
    val tabNavigator = LocalTabNavigator.current
    val selected = tabNavigator.current == tab
    val appState = LocalAppState.current

    Box {
        NavigationRailItem(
            label = {
                Text(tab.options.title)
            },
            alwaysShowLabel = true,
            selected = selected,
            onClick = {
                appState.onTabSelected(tab)
            },
            icon = {
                BadgedBox(
                    badge = {
                        if (notificationCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = remember(notificationCount) {
                                        notificationCount.toString()
                                    },
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                ) {
                    tab.options.icon?.let {
                        Icon(
                            painter = it,
                            contentDescription = tab.options.title,
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun TabNavigationItem(tab: Tab) {
    val appState = LocalAppState.current
    val tabNavigator = LocalTabNavigator.current
    val selected = tabNavigator.current == tab

    NavigationRailItem(
        label = { Text(tab.options.title) },
        alwaysShowLabel = true,
        selected = selected,
        onClick = {
            appState.onTabSelected(tab)
        },
        icon = {
            Icon(
                painter = tab.options.icon ?: return@NavigationRailItem,
                contentDescription = tab.options.title,
            )
        },
    )
}
