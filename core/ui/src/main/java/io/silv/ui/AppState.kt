package io.silv.ui

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

val LocalAppState = compositionLocalOf<AppState> { error("App State not yet provided") }

@Composable
fun rememberAppState(
    windowSizeClass: WindowSizeClass,
    bottomBarVisibilityChannel: Channel<Boolean>,
    networkConnectivity: NetworkConnectivity,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) = remember {
    AppState(
        windowSizeClass = windowSizeClass,
        bottomBarVisibilityChannel = bottomBarVisibilityChannel,
        connectivity = networkConnectivity,
        scope = coroutineScope
    )
}

@Stable
class AppState(
    val windowSizeClass: WindowSizeClass,
    val bottomBarVisibilityChannel: Channel<Boolean>,
    val scope: CoroutineScope,
    connectivity: NetworkConnectivity,
) {
    val shouldShowBottomBar: Boolean
        get() = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    val shouldShowNavRail: Boolean
        get() = !shouldShowBottomBar


    val isOffline = connectivity.online
        .map(Boolean::not)
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )
}