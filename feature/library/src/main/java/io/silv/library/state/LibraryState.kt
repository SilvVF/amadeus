package io.silv.library.state

import androidx.compose.runtime.Stable
import io.silv.domain.manga.model.MangaWithChapters
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Stable
data class LibraryTag(
    val name: String,
    val id: String,
    val selected: Boolean
)

sealed interface LibraryError {
    data object NoFavoritedChapters: LibraryError
    data class Generic(val reason: String): LibraryError
}


sealed interface LibraryState {

    data object Loading: LibraryState

    data class Error(
        val error: LibraryError
    ): LibraryState

    data class Success(
        val filteredMangaWithChapters: ImmutableList<MangaWithChapters> = persistentListOf(),
        val mangaWithChapters: ImmutableList<MangaWithChapters> = persistentListOf(),
        val filteredTagIds: ImmutableList<String> = persistentListOf(),
        val filteredText: String = "",
    ): LibraryState {
        val libraryTags = mangaWithChapters
            .flatMap { it.manga.tagToId.entries }
            .toSet()
            .map { (name, id) ->
                LibraryTag(name, id, filteredTagIds.isEmpty() || filteredTagIds.contains(id))
            }
            .toImmutableList()

        val hasFilters = filteredTagIds.isNotEmpty() || filteredText.isNotBlank()

        val isLibraryEmpty = mangaWithChapters.isEmpty()
    }

    val success: Success?
        get() = (this as? Success)
}

sealed class LibraryEvent {}

@Stable
data class LibraryActions(
    val clearTagFilter: () -> Unit = {},
    val filterByTag: (id: String) -> Unit = {_-> },
    val searchChanged: (search: String) -> Unit = {_ ->},
    val searchOnMangaDex: (query: String) -> Unit = { _ -> },
    val navigateToExploreTab: () -> Unit = {},
)
