package io.silv.library.state

import androidx.compose.runtime.Stable
import io.silv.common.model.Download
import io.silv.domain.chapter.model.Chapter
import io.silv.domain.manga.model.Manga
import io.silv.domain.manga.model.MangaWithChapters
import io.silv.library.UiChapterUpdate
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

data class LibraryState(
    val updatingLibrary: Boolean = false,
    val bookmarkedChapters: ImmutableList<Pair<Manga, ImmutableList<Chapter>>> = persistentListOf(),
    val updates: ImmutableList<Pair<Int, ImmutableList<UiChapterUpdate>>> = persistentListOf(),
    val libraryMangaState: LibraryMangaState = LibraryMangaState.Loading
)

sealed interface LibraryMangaState {

    data object Loading: LibraryMangaState

    data class Error(
        val error: LibraryError
    ): LibraryMangaState

    data class Success(
        val mangaWithChapters: ImmutableList<MangaWithChapters> = persistentListOf(),
        val filteredTagIds: ImmutableList<String> = persistentListOf(),
        val filteredText: String = "",
        val updatingLibrary: Boolean = false,
    ): LibraryMangaState {

        val filteredMangaWithChapters = mangaWithChapters
            .filter { (manga, _) ->
                filteredText.isBlank() || (manga.alternateTitles.values + manga.titleEnglish)
                .any { title -> filteredText.lowercase() in title.lowercase() }
            }
            .filter { (manga, _) -> filteredTagIds.isEmpty() || filteredTagIds.any { manga.tagIds.contains(it) } }
            .toImmutableList()

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
    val refreshUpdates: () -> Unit = {},
    val markUpdatesAsSeen: (mangaId: String, chapterId: String) -> Unit = { _, _ ->},
    val onDownload: (mangaId: String, chapterId: String) -> Unit = {_, _ ->},
    val onStartDownloadNow: (download: Download) -> Unit = {},
    val onCancelDownload: (download: Download) -> Unit = {},
    val onDeleteDownloadedChapter: (mangaId: String, chapterId: String) -> Unit = {_, _ ->},
    val toggleChapterRead: (chapterId: String) -> Unit = {},
    val toggleChapterBookmark: (chapterId: String) -> Unit = {},
    val pauseAllDownloads: () -> Unit = {},
    val updateMangaTrackedAfter: (mangaId: String, chapter: Chapter) -> Unit = { _, _ -> },
)
