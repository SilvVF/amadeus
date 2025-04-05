package io.silv.domain.chapter.interactor

import io.silv.common.model.ProgressState
import io.silv.domain.chapter.model.Chapter
import io.silv.domain.chapter.repository.ChapterRepository
import io.silv.domain.manga.model.MangaUpdate

class ChapterHandler(
    private val chapterRepository: ChapterRepository,
) {
    suspend fun refreshList(mangaId: String) {
       runCatching{ chapterRepository.refetchChapters(mangaId) }
    }

    suspend fun toggleChapterBookmarked(id: String) =
        runCatching {
            chapterRepository.updateChapter(id) { chapter ->
                chapter.copy(
                    bookmarked = !chapter.bookmarked,
                )
            }!!
        }

    suspend fun updateLastReadPage(id: String, page: Int) =
        runCatching {
            chapterRepository.updateChapter(id) {
                it.copy(
                    lastReadPage = page,
                )
            }
        }

    suspend fun toggleReadOrUnread(id: String) =
        runCatching {
            chapterRepository.updateChapter(id) {
                it.copy(
                    lastReadPage =
                    when (it.progress) {
                        ProgressState.Finished -> null
                        ProgressState.NotStarted, ProgressState.Reading -> it.pages
                    },
                )
            }!!
        }
}
