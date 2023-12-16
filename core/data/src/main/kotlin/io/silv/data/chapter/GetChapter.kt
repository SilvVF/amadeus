package io.silv.data.chapter

import io.silv.database.entity.chapter.ChapterEntity
import kotlinx.coroutines.flow.firstOrNull

internal class GetChapter(
    private val chapterRepository: ChapterRepository
) {

    suspend fun await(id: String): ChapterEntity? {
        return chapterRepository.getChapterById(id).firstOrNull()
    }
}