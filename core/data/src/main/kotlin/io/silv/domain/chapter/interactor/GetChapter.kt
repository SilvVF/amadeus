package io.silv.domain.chapter.interactor

import io.silv.domain.chapter.model.Chapter
import io.silv.domain.chapter.repository.ChapterRepository
import kotlinx.coroutines.flow.Flow

class GetChapter(
    private val chapterRepository: ChapterRepository,
) {
    suspend fun await(id: String): Chapter? {
        return chapterRepository.getChapterById(id) ?: chapterRepository.refetchChapter(id)
    }

    fun subscribe(id: String): Flow<Chapter> = chapterRepository.observeChapterById(id)
}
