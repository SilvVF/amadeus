package io.silv.manga.repositorys.chapter

import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.sync.Syncable
import kotlinx.coroutines.flow.Flow

/**
 * Repository responsible for handling Chapters that have been saved to the local db.
 */
interface ChapterEntityRepository: Syncable {

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

