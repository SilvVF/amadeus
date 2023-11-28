package io.silv.common.model


sealed interface LoadState {
    object Refreshing: LoadState
    object Loading: LoadState
    object None: LoadState
}
