package io.silv.manga.domain.repositorys.base


sealed interface PagedLoadState {
    object Refreshing: PagedLoadState
    object Loading: PagedLoadState
    object None: PagedLoadState
    object End: PagedLoadState
    data class Error(val throwable: Throwable): PagedLoadState
}

fun toBool(state: PagedLoadState): Boolean {
    return when (state) {
        PagedLoadState.Loading, PagedLoadState.Refreshing -> true
        PagedLoadState.None, PagedLoadState.End, is PagedLoadState.Error -> false
    }
}

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