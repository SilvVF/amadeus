package io.silv.amadeus

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel

object NavHost : Screen {

    @IgnoredOnParcel
    internal val bottomBarVisibility = Channel<Boolean>(UNLIMITED)

    @IgnoredOnParcel
    internal val globalSearchChannel = Channel<String?>(UNLIMITED)

    @SuppressLint("UnusedContentLambdaTargetStateParameter")
    @Composable
    override fun Content() {
        val appState = LocalAppState.current

        val visibilityChannel by produceState(initialValue = true) {
            bottomBarVisibility.receiveAsFlow().collectLatest { value = it }
        }

        val lifecycleOwner = LocalLifecycleOwner.current

        TabNavigator(
            tab = ExploreTab,
        ) { tabNavigator ->

            LaunchedEffect(lifecycleOwner.lifecycle, globalSearchChannel) {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    withContext(Dispatchers.Main.immediate) {
                        globalSearchChannel.receiveAsFlow()
                            .collect { query ->

                                Log.d("NavHost", "calling global search $query")
                                ExploreTab.onSearch(query, tabNavigator)
                            }
                    }
                }
            }
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0),
                bottomBar = {
                    AnimatedVisibility(
                        visible = appState.shouldShowBottomBar && visibilityChannel,
                        enter = slideInVertically { it },
                        exit = slideOutVertically { it },
                    ) {
                        AmadeusBottomBar(
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
            ) { incoming ->
                Row {
                    if (appState.shouldShowNavRail && visibilityChannel) {
                        AmadeusNavRail()
                    }
                    Box(
                        Modifier
                            .padding(incoming)
                            .consumeWindowInsets(incoming),
                    ) {
                        CrossfadeTransition(
                            navigator = tabNavigator
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CrossfadeTransition(
    navigator: TabNavigator,
    modifier: Modifier = Modifier,
    animationSpec: FiniteAnimationSpec<Float> = tween(),
    label: String = "Crossfade",
) {
    navigator.saveableState("currentTab") {
        Crossfade(
            targetState = navigator.current,
            animationSpec = animationSpec,
            modifier = modifier,
            label = label
        ) { tab ->
            tab.Content()
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
