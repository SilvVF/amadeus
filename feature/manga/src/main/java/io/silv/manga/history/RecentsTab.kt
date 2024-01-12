package io.silv.manga.history

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.util.fastAll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.ScreenTransition
import io.silv.manga.download.DownloadQueueScreen
import io.silv.ui.ReselectTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext

val LocalTopLevelNavigator = compositionLocalOf<Navigator> { error("Not provided.") }

object RecentsTab: ReselectTab {

    private val recentsReselectChannel = Channel<Unit>(UNLIMITED)

    override suspend fun onReselect(navigator: Navigator) {
        recentsReselectChannel.send(Unit)
    }

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 2u,
            title = "Recents",
            icon =  rememberVectorPainter(image = Icons.TwoTone.History)
        )

    @Composable
    override fun Content() {

        val topLevelNavigator = LocalNavigator.currentOrThrow
        val lifecycleOwner = LocalLifecycleOwner.current

        Navigator(RecentsScreen()) { navigator ->

            CompositionLocalProvider(LocalTopLevelNavigator provides topLevelNavigator) {
                ScreenTransition(
                    navigator = navigator,
                    modifier = Modifier.fillMaxSize(),
                    content = {
                        navigator.saveableState("screen-transition", it) {
                            it.Content()
                        }
                    },
                    transition = {
                        val enter = if (this.targetState is DownloadQueueScreen) {
                            slideInVertically { it }
                        } else {
                            fadeIn()
                        }
                        val exit = if (this.targetState is DownloadQueueScreen) {
                            fadeOut()
                        } else {
                            slideOutVertically { it }
                        }
                        enter togetherWith exit
                    }
                )
            }

            LaunchedEffect(recentsReselectChannel, lifecycleOwner.lifecycle) {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    withContext(Dispatchers.Main.immediate) {
                        recentsReselectChannel.receiveAsFlow().collectLatest {
                            if (navigator.items.fastAll { it !is DownloadQueueScreen }) {
                                navigator.push(DownloadQueueScreen())
                            } else {
                                navigator.pop()
                            }
                        }
                    }
                }
            }
        }
    }
}
