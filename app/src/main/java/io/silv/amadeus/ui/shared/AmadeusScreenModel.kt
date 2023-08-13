package io.silv.amadeus.ui.shared

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

abstract class AmadeusScreenModel<EVENT>: ScreenModel {

    protected val mutableEvents = Channel<EVENT>()

    val events = mutableEvents.receiveAsFlow()

    protected fun <T> Flow<T>.stateInUi(
        initialValue: T
    ) = this.stateIn(
        coroutineScope,
        SharingStarted.WhileSubscribed(5000), initialValue
    )

    // For more details, check: https://gist.github.com/marcellogalhardo/2a1ec56b7d00ba9af1ec9fd3583d53dc
    protected fun <T> SavedStateHandle.getMutableStateFlow(
        key: String,
        initialValue: T
    ): MutableStateFlow<T> {
        val liveData = getLiveData(key, initialValue)
        val stateFlow = MutableStateFlow(initialValue)

        val observer = Observer<T> { value ->
            if (value != stateFlow.value) {
                stateFlow.value = value
            }
        }
        liveData.observeForever(observer)

        stateFlow.onCompletion {
            withContext(Dispatchers.Main.immediate) {
                liveData.removeObserver(observer)
            }
        }.onEach { value ->
            withContext(Dispatchers.Main.immediate) {
                if (liveData.value != value) {
                    liveData.value = value
                }
            }
        }.launchIn(coroutineScope)

        return stateFlow
    }
}

/**
 * Observe [AmadeusScreenModel.events] in a Compose [LaunchedEffect].
 * @param lifecycleState [Lifecycle.State] in which [event] block runs.
 * [orbit_Impl](https://github.com/orbit-mvi/orbit-mvi/blob/main/orbit-compose/src/main/kotlin/org/orbitmvi/orbit/compose/ContainerHostExtensions.kt)
 */
@SuppressLint("ComposableNaming")
@Composable
fun <EVENT> AmadeusScreenModel<EVENT>.collectEvents(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    event: (suspend (event: EVENT) -> Unit)
) {
    val sideEffectFlow = events
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(sideEffectFlow, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(lifecycleState) {
            sideEffectFlow.collect { event(it) }
        }
    }
}