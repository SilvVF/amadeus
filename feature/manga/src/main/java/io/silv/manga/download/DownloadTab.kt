package io.silv.manga.download

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Download
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.FadeTransition
import io.silv.ui.CenterBox
import io.silv.ui.ReselectTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext

object RecentsTab: ReselectTab {

    internal val recentsReselectChannel = Channel<Unit>(UNLIMITED)

    override suspend fun onReselect(navigator: Navigator) {
        recentsReselectChannel.trySend(Unit)
    }

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 2u,
            title = "Downloads",
            icon =  rememberVectorPainter(image = Icons.TwoTone.Download)
        )

    @Composable
    override fun Content() {

        val lifecycleOwner = LocalLifecycleOwner.current

        Navigator(
            HistoryScreen()
        ) { navigator ->

            LaunchedEffect(recentsReselectChannel, lifecycleOwner.lifecycle) {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    withContext(Dispatchers.Main.immediate) {
                        recentsReselectChannel.receiveAsFlow().collect {
                            if (navigator.lastOrNull is DownloadQueueScreen) {
                                navigator.popUntil { it is HistoryScreen }
                            } else {
                                navigator.push(DownloadQueueScreen())
                            }
                        }
                    }
                }
            }

            FadeTransition(navigator)
        }
    }
}

class HistoryScreen: Screen {
    @Composable
    override fun Content() {
        RecentsScreenContent()
    }
}

@Composable
fun RecentsScreenContent() {
    CenterBox(Modifier.fillMaxSize()) {
        Text("History")
    }
}