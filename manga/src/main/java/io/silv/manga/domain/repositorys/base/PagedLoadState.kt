package io.silv.manga.domain.repositorys.base


sealed interface LoadState {
    object Refreshing: LoadState
    object Loading: LoadState
    object None: LoadState
}

fun toBool(state: LoadState): Boolean {
    return when (state) {
         LoadState.Refreshing, LoadState.Loading -> true
        LoadState.None ->  false
    }
}