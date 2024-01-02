package io.silv.explore

import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.FadeTransition
import io.silv.ui.GlobalSearchTab
import io.silv.ui.LocalAppState
import io.silv.ui.ReselectTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

object ExploreTab : ReselectTab, GlobalSearchTab {


    internal val reselectChannel = Channel<Unit>()

    internal val searchChannel = Channel<String>(capacity = 1)

    override suspend fun onSearch(query: String?, navigator: TabNavigator) {
        query?.let{ searchChannel.trySend(query) }
        navigator.current = this
    }

    override suspend fun onReselect(navigator: Navigator) {
        Log.d("Explore", "Sending reselect event")
        reselectChannel.send(Unit)
    }

    @OptIn(ExperimentalAnimationGraphicsApi::class)
    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_browse_enter)
            return TabOptions(
                index = 0u,
                title = "Home",
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun Content() {
        val appState = LocalAppState.current

        Navigator(
            ExploreScreen()
        ) { navigator ->

            LaunchedEffect(appState) {
                snapshotFlow { navigator.lastEvent }.onEach { event ->
                    if (navigator.items.size > 1) {
                        appState.bottomBarVisibilityChannel.send(false)
                    } else {
                        appState.bottomBarVisibilityChannel.send(true)
                    }
                }
                    .collect()
            }

            FadeTransition(navigator)
        }
    }
}
