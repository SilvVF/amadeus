package io.silv.amadeus.ui.shared

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

abstract class AmadeusScreenModel<EVENT>: ScreenModel {

    protected val mutableEvents = Channel<EVENT>()

    val events = mutableEvents.receiveAsFlow()

    protected fun <T> Flow<T>.stateInUi(
        initialValue: T
    ) = this.stateIn(
        coroutineScope,
        SharingStarted.WhileSubscribed(5000),
        initialValue
    )
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