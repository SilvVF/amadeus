package io.silv.library

import io.silv.common.model.ProgressState
import io.silv.model.SavableChapter
import io.silv.model.SavableManga

sealed interface Update {
    data class Chapter(val chapterId: String, val manga: SavableManga) : Update

    data class Volume(val chapterId: String, val manga: SavableManga) : Update
}

data class LibraryManga(
    val savableManga: SavableManga,
    val chapters: List<SavableChapter>,
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
