package io.silv.data.chapter

import io.silv.data.util.Syncable
import io.silv.database.entity.chapter.ChapterEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository responsible for handling Chapters that have been saved to the local db.
 */
interface ChapterRepository: Syncable {

    suspend fun bookmarkChapter(id: String)

    suspend fun updateChapter(id: String, copy: (ChapterEntity) -> ChapterEntity): ChapterEntity

    suspend fun updateLastReadPage(chapterId: String, page: Int, lastPage: Int)

    suspend fun saveChapters(ids: List<String>): Boolean

    suspend fun saveChapter(chapterEntity: ChapterEntity)

    suspend fun refetchChapter(id: String): ChapterEntity?

    fun observeChaptersByMangaId(mangaId: String): Flow<List<ChapterEntity>>

    suspend fun getChapterById(id: String): ChapterEntity?

    fun observeChapterById(id: String): Flow<ChapterEntity>

    fun observeChapters(): Flow<List<ChapterEntity>>
}

