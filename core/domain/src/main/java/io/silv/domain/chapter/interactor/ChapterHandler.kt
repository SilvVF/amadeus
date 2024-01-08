package io.silv.domain.chapter.interactor

import io.silv.common.model.ProgressState
import io.silv.domain.chapter.repository.ChapterRepository

class ChapterHandler(
    private val chapterRepository: ChapterRepository,
) {
    suspend fun toggleChapterBookmarked(id: String) =
        runCatching {
            chapterRepository.updateChapter(id) { chapter ->
                chapter.copy(
                    bookmarked = !chapter.bookmarked,
                )
            }!!
        }

    suspend fun updateLastReadPage(id: String, page: Int, isLast: Boolean) =
        runCatching {
            chapterRepository.updateChapter(id) {
                it.copy(
                    lastReadPage = page,
                    progress = if (isLast) ProgressState.Finished else ProgressState.Reading,
                )
            }
        }

    suspend fun toggleReadOrUnread(id: String) =
        runCatching {
            chapterRepository.updateChapter(id) {
                it.copy(
                    progress =
                    when (it.progress) {
                        ProgressState.Finished -> ProgressState.NotStarted
                        ProgressState.NotStarted, ProgressState.Reading -> ProgressState.Finished
                    },
                )
            }!!
        }
}
