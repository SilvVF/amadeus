package io.silv.amadeus.ui.screens.search

import io.silv.manga.domain.models.DomainManga

sealed interface SearchMangaUiState {

    object WaitingForQuery: SearchMangaUiState

    object Refreshing: SearchMangaUiState

    sealed class Success(
        open val results: List<DomainManga> = emptyList(),
    ): SearchMangaUiState {

        data class Idle(
            override val results: List<DomainManga>,
        ): Success(results)

        data class Loading(
            override val results: List<DomainManga>,
        ): Success(results)

        data class EndOfPagination(
            override val results: List<DomainManga>,
        ): Success(results)
    }
}