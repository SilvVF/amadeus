package io.silv.ui

import android.util.Log
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import io.silv.common.model.NetworkConnectivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val LocalAppState = compositionLocalOf<AppState> { error("App State not yet provided") }

@Composable
fun rememberAppState(
    windowSizeClass: WindowSizeClass,
    networkConnectivity: NetworkConnectivity,
    searchTab: GlobalSearchTab,
    baseScreen: Screen,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) = remember {
    AppState(
        windowSizeClass = windowSizeClass,
        connectivity = networkConnectivity,
        searchTab = searchTab,
        baseScreen = baseScreen,
        scope = coroutineScope
    )
}

@Immutable
@Stable
class AppState(
    val windowSizeClass: WindowSizeClass,
    val scope: CoroutineScope,
    searchTab: GlobalSearchTab,
    baseScreen: Screen,
    connectivity: NetworkConnectivity,
) {
    private val globalSearchCh = Channel<String>()

    var navigator by mutableStateOf<Navigator?>(null)
    var tabNavigator by mutableStateOf<TabNavigator?>(null)

    val bottomBarVisible by derivedStateOf {
        navigator?.lastItemOrNull == baseScreen
    }

    init {
        scope.launch {
            combine(
                snapshotFlow { navigator }.filterNotNull(),
                snapshotFlow { tabNavigator }.filterNotNull(),
                ::Pair
            ).collectLatest { (nav, tabNav) ->
                for (query in globalSearchCh) {
                    searchTab.searchCh.send(query)
                    tabNav.current = searchTab
                }
            }
        }
    }

    val shouldShowBottomBar: Boolean by derivedStateOf {
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
                && bottomBarVisible
    }

    val shouldShowNavRail: Boolean by derivedStateOf {
        !shouldShowBottomBar && bottomBarVisible
    }

    val isOffline =
        connectivity.online
            .map(Boolean::not)
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false,
            )

    fun onTabSelected(tab: Tab) {
        if (tabNavigator?.current == tab
            && tab is ReselectTab
        ) {
            scope.launch {
                Log.d("Reselect", "Sending Reselect event to $tab")
                tab.reselectCh.send(Unit)
            }
        } else {
            tabNavigator?.current = tab
        }
    }

    fun searchGlobal(query: String?) {
        scope.launch(Dispatchers.Main.immediate) {
            globalSearchCh.send(query ?: "")
        }
    }
}
