package io.silv.library

import io.silv.common.emptyImmutableList
import io.silv.common.model.Download
import io.silv.common.model.ProgressState
import io.silv.model.SavableChapter
import io.silv.model.SavableManga
import kotlinx.collections.immutable.ImmutableList

sealed interface Update {
    data class Chapter(val chapterId: String, val manga: SavableManga) : Update

    data class Volume(val chapterId: String, val manga: SavableManga) : Update
}

data class LibraryChapter(
    val chapter: SavableChapter,
    val download: Download? = null
)

data class LibraryState(
    val libraryManga: ImmutableList<LibraryManga> = emptyImmutableList(),
    val updates: ImmutableList<Update> = emptyImmutableList(),
    val userLists: ImmutableList<SavableManga> = emptyImmutableList(),
    val bookmarkedChapters: ImmutableList<LibraryChapter> = emptyImmutableList()
)

data class LibraryManga(
    val savableManga: SavableManga,
    val chapters: ImmutableList<SavableChapter>,
) {
    val unread: Int
        get() = chapters.count { it.progress != ProgressState.Finished }

    val lastReadChapter: SavableChapter?
        get() =
            chapters
                .filter { it.progress == ProgressState.Reading || it.progress == ProgressState.NotStarted }
                .minByOrNull { if (it.chapter != 1L) it.chapter else Long.MAX_VALUE }
                ?: chapters.firstOrNull()
}
