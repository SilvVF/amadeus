package io.silv.domain.chapter

import io.silv.data.chapter.ChapterRepository
import io.silv.model.SavableChapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetSavableChapter(
    private val chapterRepository: ChapterRepository,
) {
    suspend fun await(id: String): SavableChapter? {
        return chapterRepository.getChapterById(id)?.let(::SavableChapter)
            ?: chapterRepository.refetchChapter(id)?.let(::SavableChapter)
    }

    fun subscribe(id: String): Flow<SavableChapter> =
        chapterRepository.observeChapterById(id).map { entity ->
            SavableChapter(entity)
        }
}
