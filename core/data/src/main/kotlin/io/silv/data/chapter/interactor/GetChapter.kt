package io.silv.data.chapter.interactor

import io.silv.data.chapter.Chapter
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
