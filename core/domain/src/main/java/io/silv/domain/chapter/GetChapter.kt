package io.silv.domain.chapter

import io.silv.data.chapter.ChapterRepository
import io.silv.model.SavableChapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class GetChapter(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(id: String): SavableChapter? {
        chapterRepository.getChapterById(id).first()
            .let { it?.let(::SavableChapter) }
            .takeIf { it != null }
            ?.let { return it }

        return chapterRepository.refetchChapter(id)?.let(::SavableChapter)
    }

    fun subscribe(id: String): Flow<SavableChapter?> = chapterRepository.getChapterById(id).map { it?.let(::SavableChapter) }
}