package io.silv.data.chapter.interactor

import io.silv.common.log.asLog
import io.silv.common.log.logcat
import io.silv.common.model.ProgressState
import io.silv.data.chapter.repository.ChapterRepository


class ChapterHandler(
    private val chapterRepository: ChapterRepository,
) {
    suspend fun refreshList(mangaId: String) {
       runCatching { chapterRepository.refetchChapters(mangaId) }
           .onFailure {
               logcat { it.asLog() }
           }
           .onSuccess {
               logcat { "fetched ${chapterRepository.getChaptersByMangaId(mangaId)}" }
           }
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
