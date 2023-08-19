package io.silv.manga.repositorys


sealed interface LoadState {
    object Refreshing: LoadState
    object Loading: LoadState
    object None: LoadState
}
