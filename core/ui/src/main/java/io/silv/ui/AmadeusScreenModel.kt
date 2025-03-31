package io.silv.ui

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.ScreenModelStore
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext

abstract class EventStateScreenModel<EVENT, STATE>(initialState: STATE) : StateScreenModel<STATE>(
    initialState
) {
    private val mutableEvents = Channel<EVENT>(UNLIMITED)

    val events = mutableEvents.receiveAsFlow()

    protected suspend fun sendEvent(event: EVENT) {
        withContext(Dispatchers.Main.immediate) {
            mutableEvents.send(event)
        }
    }

    protected fun <T> Flow<T>.stateInUi(initialValue: T) =
        this.stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            initialValue,
        )
}

val ScreenModel.ioCoroutineScope: CoroutineScope
    get() =
        ScreenModelStore.getOrPutDependency(
            this,
            name = "ScreenModelIoCoroutineScope",
            factory = { key ->
                CoroutineScope(Dispatchers.IO + SupervisorJob()) + CoroutineName(key)
            },
            onDispose = { scope -> scope.cancel() },
        )

abstract class EventScreenModel<EVENT> : ScreenModel {
    protected val mutableEvents = Channel<EVENT>()

    val events = mutableEvents.receiveAsFlow()

    protected fun <T> Flow<T>.stateInUi(initialValue: T) =
        this.stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            initialValue,
        )
}

@Composable
fun <STATE> StateScreenModel<STATE>.collectAsStateWithLifeCycle(): State<STATE> {
    return state.collectAsStateWithLifecycle()
}

/**
 * Observe [EventScreenModel.events] in a Compose [LaunchedEffect].
 * @param lifecycleState [Lifecycle.State] in which [event] block runs.
 * [orbit_Impl](https://github.com/orbit-mvi/orbit-mvi/blob/main/orbit-compose/src/main/kotlin/org/orbitmvi/orbit/compose/ContainerHostExtensions.kt)
 */
@SuppressLint("ComposableNaming")
@Composable
fun <EVENT> EventScreenModel<EVENT>.collectEvents(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    event: (suspend (event: EVENT) -> Unit),
) {
    val sideEffectFlow = events
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(sideEffectFlow, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(lifecycleState) {
            withContext(Dispatchers.Main.immediate) {
                sideEffectFlow.collect { event(it) }
            }
        }
    }
}

/**
 * Observe [EventStateScreenModel.events] in a Compose [LaunchedEffect].
 * @param lifecycleState [Lifecycle.State] in which [event] block runs.
 * [orbit_Impl](https://github.com/orbit-mvi/orbit-mvi/blob/main/orbit-compose/src/main/kotlin/org/orbitmvi/orbit/compose/ContainerHostExtensions.kt)
 */
@SuppressLint("ComposableNaming")
@Composable
fun <EVENT> EventStateScreenModel<EVENT, *>.collectEvents(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    event: (suspend (event: EVENT) -> Unit),
) {
    val sideEffectFlow = events
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(sideEffectFlow, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(lifecycleState) {
            withContext(Dispatchers.Main.immediate) {
                sideEffectFlow.collect { event(it) }
            }
        }
    }
}
