package io.silv.amadeus.ui.composables

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import io.silv.amadeus.ui.screens.home.PaginatedListState
import io.silv.amadeus.ui.screens.search.SearchMangaUiState
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.domain.repositorys.base.PagedLoadState

@Composable
fun PageLoaderList(
    loadState: PaginatedListState<List<List<SavableManga>>>,
    state: LazyListState,
    loadNextPage: suspend () -> Unit,
) {
    val load by rememberUpdatedState(loadNextPage)
    LaunchedEffect(state) {
        snapshotFlow {  state.firstVisibleItemIndex to loadState }.collect {  (idx, state)  ->
           when(state) {
               is PaginatedListState.Error -> Unit
               PaginatedListState.Refreshing -> Unit
               is PaginatedListState.Success -> {
                   if (!state.end && !state.loading && idx >= state.data.size - 10) {
                       load()
                   }
               }
           }
        }
    }
}

@Composable
fun PageLoader(
    loadState: PaginatedListState<List<SavableManga>>,
    state: LazyListState,
    loadNextPage: suspend () -> Unit,
) {
    val load by rememberUpdatedState(loadNextPage)
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex to loadState}.collect { (idx, state)  ->
            when(state) {
                is PaginatedListState.Error -> Unit
                PaginatedListState.Refreshing -> Unit
                is PaginatedListState.Success -> {
                    if (!state.end && !state.loading && idx >= state.data.size - 10) {
                        load()
                    }
                }
            }
        }
    }
}

@Composable
fun PageLoaderGrid(
    loadState: PaginatedListState<List<SavableManga>>,
    state: LazyGridState,
    loadNextPage: suspend () -> Unit,
) {
    val load by rememberUpdatedState(loadNextPage)
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex to loadState }.collect { (idx, state) ->
            when(state) {
                is PaginatedListState.Error -> Unit
                PaginatedListState.Refreshing -> Unit
                is PaginatedListState.Success -> {
                    if (!state.end && !state.loading && idx >= state.data.size - 10) {
                        load()
                    }
                }
            }
        }
    }
}

@Composable
fun PageLoader(
    loadState: PagedLoadState,
    state: LazyListState,
    listSize: Int,
    loadNextPage: suspend () -> Unit,
) {
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }.collect { idx ->
            if (loadState !is PagedLoadState.Error || loadState !is PagedLoadState.Loading && idx >= listSize - 10) {
                loadNextPage()
            }
        }
    }
}

@Composable
fun PageLoader(
    loadState: PagedLoadState,
    state: LazyGridState,
    list: SearchMangaUiState,
    loadNextPage: suspend () -> Unit,
) {
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }.collect { idx ->
            if (loadState !is PagedLoadState.Error || loadState !is PagedLoadState.Loading && idx >= ((list as? SearchMangaUiState.Success)?.results?.size ?: 0) - 10) {
                loadNextPage()
            }
        }
    }
}

@Composable
fun PageLoader(
    loadState: PagedLoadState,
    state: LazyGridState,
    listSize: Int,
    loadNextPage: suspend () -> Unit,
) {
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }.collect { idx ->
            if (loadState !is PagedLoadState.Error || loadState !is PagedLoadState.Loading && idx >= listSize - 10) {
                loadNextPage()
            }
        }
    }
}
