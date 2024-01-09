package io.silv.manga.download

import android.util.Log
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.History
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.util.fastAll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.ScreenTransition
import io.silv.domain.history.HistoryRepository
import io.silv.model.HistoryWithRelations
import io.silv.ui.CenterBox
import io.silv.ui.ReselectTab
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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


data class RecentsState(
    val history: ImmutableList<HistoryWithRelations> = persistentListOf()
)

class RecentsScreenModel(
    private val historyRepository: HistoryRepository
): StateScreenModel<RecentsState>(RecentsState()) {

    init {
        screenModelScope.launch {
            val history = historyRepository.getHistory().toImmutableList()
            mutableState.update {
                it.copy(history = history)
            }
            Log.d("HISTORY", history.toString())
        }
    }
}

class RecentsScreen: Screen {

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<RecentsScreenModel>()

        val state by screenModel.state.collectAsStateWithLifecycle()

        CenterBox(Modifier.fillMaxSize()) {
            LazyColumn {
                items(
                    items = state.history,
                    key = { it.id }
                ) {
                    Text(it.toString(), modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun RecentsScreenContent() {
    CenterBox(Modifier.fillMaxSize()) {
        Text("History")
    }
}