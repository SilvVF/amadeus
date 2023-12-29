package io.silv.domain.chapter.interactor

import io.silv.domain.chapter.model.Chapter
import io.silv.domain.chapter.repository.ChapterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class GetBookmarkedChapters(
    private val chapterRepository: ChapterRepository,
) {

    fun subscribe(): Flow<List<Chapter>> =
        chapterRepository.observeBookmarkedChapters()

    suspend fun await() = subscribe().firstOrNull()
}