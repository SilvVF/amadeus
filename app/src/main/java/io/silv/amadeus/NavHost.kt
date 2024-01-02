package io.silv.amadeus

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import io.silv.explore.ExploreTab
import io.silv.library.LibraryTab
import io.silv.manga.download.RecentsTab
import io.silv.ui.LocalAppState
import io.silv.ui.ReselectTab
import io.silv.ui.layout.Scaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut

object NavHost : Screen {

    @IgnoredOnParcel
    internal val globalSearchChannel = Channel<String?>(UNLIMITED)

    @IgnoredOnParcel
    internal val bottomBarVisibility = Channel<Boolean>()

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
            tab = ExploreTab,
        ) { tabNavigator ->
            CompositionLocalProvider(LocalNavigator provides nav) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0),
                    startBar = {
                        if (appState.shouldShowNavRail && bottomBarVisible) {
                            AmadeusNavRail()
                        }
                    },
                    bottomBar = {
                        if (appState.shouldShowBottomBar && bottomBarVisible) {
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

            LaunchedEffect(lifecycleOwner.lifecycle, globalSearchChannel) {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    withContext(Dispatchers.Main.immediate) {
                        globalSearchChannel.receiveAsFlow()
                            .collect { query ->
                                if (query != null) {
                                    ExploreTab.searchChannel.send(query)
                                }
                            }
                    }
                }
            }
        }
    }
}




@Composable
fun AmadeusBottomBar(modifier: Modifier = Modifier) {
    BottomAppBar(modifier) {
        TabNavigationItem(ExploreTab)
        TabNavigationItem(LibraryTab)
        TabNavigationItem(RecentsTab)
    }
}

@Composable
fun AmadeusNavRail(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    AnimatedVisibility(visible = visible) {
        NavigationRail(modifier) {
            TabNavigationItem(ExploreTab)
            TabNavigationItem(LibraryTab)
            TabNavigationItem(RecentsTab)
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
                contentDescription = tab.options.title,
            )
        },
    )
}
