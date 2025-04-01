package io.silv.amadeus

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import io.silv.amadeus.dependency.rememberDataDependency
import io.silv.domain.update.GetUpdateCount
import io.silv.explore.ExploreTab
import io.silv.library.LibraryTab
import io.silv.manga.history.RecentsTab
import io.silv.manga.settings.MoreTab
import io.silv.ui.LocalAppState
import io.silv.ui.ReselectTab
import io.silv.ui.layout.Scaffold
import io.silv.ui.theme.LocalSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut

object NavHost : Screen {

    private fun readResolve(): Any = NavHost

    @IgnoredOnParcel
    internal val bottomBarVisibility = Channel<Boolean>()

    internal val globalSearchChannel = Channel<String?>()

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedContentLambdaTargetStateParameter")
    @Composable
    override fun Content() {
        val appState = LocalAppState.current
        val lifecycleOwner = LocalLifecycleOwner.current

        val bottomBarVisible by produceState(initialValue = true) {
            bottomBarVisibility.receiveAsFlow().collectLatest { value = it }
        }


        val nav = LocalNavigator.currentOrThrow
        TabNavigator(
            tab = LibraryTab,
        ) { tabNavigator ->
            CompositionLocalProvider(LocalNavigator provides nav) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0),
                    startBar = {
                        AnimatedVisibility(appState.shouldShowNavRail && bottomBarVisible) {
                            AmadeusNavRail()
                        }
                    },
                    bottomBar = {
                        AnimatedVisibility(appState.shouldShowBottomBar && bottomBarVisible) {
                            AmadeusBottomBar(modifier = Modifier.fillMaxWidth())
                        }
                    }
                ) { incoming ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(incoming)
                            .consumeWindowInsets(incoming)
                    ) {
                        AnimatedContent(
                            modifier = Modifier.fillMaxSize(),
                            targetState = tabNavigator.current,
                            transitionSpec = {
                                materialFadeThroughIn(
                                    initialScale = 1f,
                                    durationMillis = 200
                                ) togetherWith
                                        materialFadeThroughOut(durationMillis = 200)
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
            LaunchedEffect(globalSearchChannel, lifecycleOwner) {
                withContext(Dispatchers.Main.immediate) {
                    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        globalSearchChannel.receiveAsFlow().collectLatest {
                            ExploreTab.onSearch(it, tabNavigator)
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

    AnimatedVisibility(visible = visible) {
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
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.currentOrThrow
    val space = LocalSpacing.current

    Box {
        NavigationRailItem(
            label = {
                Text(tab.options.title)
            },
            alwaysShowLabel = true,
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
private fun TabNavigationItem(tab: ReselectTab) {
    val tabNavigator = LocalTabNavigator.current
    val selected = tabNavigator.current == tab
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.currentOrThrow

    NavigationRailItem(
        label = { Text(tab.options.title) },
        alwaysShowLabel = true,
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
                contentDescription = tab.options.title,
            )
        },
    )
}
