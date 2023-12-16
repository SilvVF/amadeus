package io.silv.domain.chapter

import io.silv.common.model.ProgressState
import io.silv.data.chapter.ChapterRepository

class ChapterHandler(
    private val chapterRepository: ChapterRepository
) {

    suspend fun toggleChapterBookmarked(id: String) = runCatching {
       chapterRepository.updateChapter(id) { entity ->
            entity.copy(
                bookmarked = !entity.bookmarked
            )
        }
    }

    suspend fun toggleReadOrUnread(id: String) = runCatching {
        chapterRepository.updateChapter(id) {
            it.copy(
                progressState = when (it.progressState) {
                    ProgressState.Finished -> ProgressState.NotStarted
                    ProgressState.NotStarted, ProgressState.Reading -> ProgressState.Finished
                }
            )
        }
    }
}