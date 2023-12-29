package io.silv.library

import io.silv.common.emptyImmutableList
import io.silv.common.model.Download
import io.silv.common.model.ProgressState
import io.silv.domain.chapter.model.Chapter
import io.silv.domain.manga.model.Manga
import kotlinx.collections.immutable.ImmutableList

sealed interface Update {
    data class Chapter(val chapterId: String, val manga: Manga) : Update

    data class Volume(val chapterId: String, val manga: Manga) : Update
}

data class LibraryChapter(
    val chapter: Chapter,
    val download: Download? = null
)

data class LibraryState(
    val libraryManga: ImmutableList<LibraryManga> = emptyImmutableList(),
    val updates: ImmutableList<Update> = emptyImmutableList(),
    val userLists: ImmutableList<Manga> = emptyImmutableList(),
    val bookmarkedChapters: ImmutableList<LibraryChapter> = emptyImmutableList()
)

data class LibraryManga(
    val savableManga: Manga,
    val chapters: ImmutableList<Chapter>,
) {
    val unread: Int
        get() = chapters.count { it.progress != ProgressState.Finished }

    val lastReadChapter: Chapter?
        get() =
            chapters
                .filter { it.progress == ProgressState.Reading || it.progress == ProgressState.NotStarted }
                .minByOrNull { if (it.chapter != 1L) it.chapter else Long.MAX_VALUE }
                ?: chapters.firstOrNull()
}
