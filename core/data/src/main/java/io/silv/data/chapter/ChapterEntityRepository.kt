package io.silv.data.chapter

import io.silv.database.entity.chapter.ChapterEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository responsible for handling Chapters that have been saved to the local db.
 */
interface ChapterEntityRepository: io.silv.data.util.Syncable {

    val loadingVolumeArtIds: Flow<List<String>>

    suspend fun bookmarkChapter(id: String)

    suspend fun updateChapter(id: String, copy: (ChapterEntity) -> ChapterEntity)

    suspend fun updateLastReadPage(chapterId: String, page: Int, lastPage: Int)

    suspend fun saveChapters(ids: List<String>): Boolean

    suspend fun saveChapter(chapterEntity: ChapterEntity)

    fun getChapters(mangaId: String): Flow<List<ChapterEntity>>

    fun getChapterById(id: String): Flow<ChapterEntity?>

    fun getAllChapters(): Flow<List<ChapterEntity>>
}

