package io.silv.manga.download

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.window.Popup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.ScreenTransition
import io.silv.ui.CenterBox
import io.silv.ui.LocalAppState
import io.silv.ui.ReselectTab
import io.silv.ui.theme.LocalSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

object RecentsTab: ReselectTab {

    internal val recentsReselectChannel = Channel<Unit>(UNLIMITED)

    private var hintShown = false
        get() = field.also { hintShown = true }

    override suspend fun onReselect(navigator: Navigator) {
        recentsReselectChannel.trySend(Unit)
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

        val lifecycleOwner = LocalLifecycleOwner.current
        val appState = LocalAppState.current
        val space = LocalSpacing.current

        var popupVisible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            if (!hintShown) {
                popupVisible = true
            }
        }

        val transition = rememberInfiniteTransition(label = "")

        val offset by transition.animateFloat(
            initialValue = with(LocalDensity.current) { -space.med.toPx() },
            targetValue = with(LocalDensity.current) { -space.large.toPx() },
            label = "",
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1500,
                    easing = FastOutLinearInEasing
                ),
                repeatMode = RepeatMode.Reverse
            )
        )

        AnimatedVisibility(popupVisible) {
            Popup(
                alignment = Alignment.BottomCenter,
                offset = IntOffset(
                    x = 0,
                    y = offset.roundToInt()
                ),
                onDismissRequest = {
                    popupVisible = false
                }
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceTint,
                    shape = CircleShape,
                ) {
                    CenterBox(modifier = Modifier.padding(4.dp)) {
                        Text("Reselect recents tab for download queue")
                    }
                }
            }
        }

        Navigator(RecentsScreen()) { navigator ->
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
                    val exit =  if (this.targetState is DownloadQueueScreen) {
                         fadeOut()
                    } else {
                        slideOutVertically { it }
                    }
                    enter togetherWith exit
                }
            )

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


class RecentsScreen: Screen {

    @Composable
    override fun Content() {



        CenterBox(Modifier.fillMaxSize()) {
            Text("History")
        }
    }
}

@Composable
private fun RecentsScreenContent() {
    CenterBox(Modifier.fillMaxSize()) {
        Text("History")
    }
}