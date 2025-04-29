package io.silv.library.state

import androidx.compose.runtime.Stable
import io.silv.common.model.Download
import io.silv.data.chapter.Chapter
import io.silv.data.manga.model.Manga
import io.silv.data.manga.model.MangaWithChapters
import io.silv.library.UiChapterUpdate



import kotlinx.datetime.LocalDateTime

@Stable
data class LibraryTag(
    val name: String,
    val id: String,
    val selected: Boolean
)

sealed interface LibraryError {
    @Stable
    data object NoFavoritedChapters: LibraryError
    @Stable
    data class Generic(val reason: String): LibraryError
}

@Stable
data class LibraryState(
    val libraryLastUpdated: LocalDateTime? = null,
    val updatingLibrary: Boolean = false,
    val bookmarkedChapters: List<Pair<Manga, List<Chapter>>> = emptyList(),
    val updates: List<Pair<Int, List<UiChapterUpdate>>> = emptyList(),
    val libraryMangaState: LibraryMangaState = LibraryMangaState.Loading
)

sealed interface LibraryMangaState {

    @Stable
    data object Loading: LibraryMangaState

    @Stable
    data class Error(
        val error: LibraryError
    ): LibraryMangaState

    @Stable
    data class Success(
        val mangaWithChapters: List<MangaWithChapters> = emptyList(),
        val filteredTagIds: List<String> = emptyList(),
        val filteredText: String = "",
        val updatingLibrary: Boolean = false,
    ): LibraryMangaState {

        val filteredMangaWithChapters = mangaWithChapters
            .filter { (manga, _) ->
                filteredText.isBlank() || (manga.alternateTitles.values + manga.titleEnglish)
                .any { title -> filteredText.lowercase() in title.lowercase() }
            }
            .filter { (manga, _) -> filteredTagIds.isEmpty() || filteredTagIds.any { manga.tagIds.contains(it) } }
            .toList()

        val libraryTags = mangaWithChapters
            .flatMap { it.manga.tagToId.entries }
            .toSet()
            .map { (name, id) ->
                LibraryTag(name, id, filteredTagIds.isEmpty() || filteredTagIds.contains(id))
            }
            .toList()

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
    val navigateToExploreTab: (query: String?) -> Unit = {},
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
