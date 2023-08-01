package io.silv.manga.domain.repositorys.base

sealed interface LoadState {
    object Refreshing: LoadState
    object Loading: LoadState
    object None: LoadState
    object End: LoadState
}

fun toBool(state: LoadState): Boolean {
    return when (state) {
        LoadState.Loading, LoadState.Refreshing -> true
        LoadState.None, LoadState.End -> false
    }
}