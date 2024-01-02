package io.silv.ui

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.silv.common.model.NetworkConnectivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val LocalAppState = compositionLocalOf<AppState> { error("App State not yet provided") }

@Composable
fun rememberAppState(
    windowSizeClass: WindowSizeClass,
    networkConnectivity: NetworkConnectivity,
    exploreSearchChannel: SendChannel<String?>,
    bottomBarVisibilityChannel: SendChannel<Boolean>,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) = remember {
    AppState(
        windowSizeClass = windowSizeClass,
        connectivity = networkConnectivity,
        scope = coroutineScope,
        exploreSearchChannel = exploreSearchChannel,
        bottomBarVisibilityChannel = bottomBarVisibilityChannel
    )
}

@Immutable
@Stable
class AppState(
    val windowSizeClass: WindowSizeClass,
    val exploreSearchChannel: SendChannel<String?>,
    val bottomBarVisibilityChannel: SendChannel<Boolean>,
    val scope: CoroutineScope,
    connectivity: NetworkConnectivity,
) {
    val shouldShowBottomBar: Boolean
        get() = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    val shouldShowNavRail: Boolean
        get() = !shouldShowBottomBar

    val isOffline =
        connectivity.online
            .map(Boolean::not)
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false,
            )

    fun searchGlobal(query: String?) {
        scope.launch {
            withContext(Dispatchers.Main.immediate) {
                exploreSearchChannel.trySend(query)
            }
        }
    }
}
