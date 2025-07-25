package io.silv.data.chapter.interactor

import io.silv.data.chapter.Chapter
import io.silv.data.chapter.repository.ChapterRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class GetBookmarkedChapters(
    private val chapterRepository: ChapterRepository,
) {

    fun subscribe(): Flow<List<Chapter>> =
        chapterRepository.observeBookmarkedChapters()

    suspend fun await() = subscribe().firstOrNull()
}