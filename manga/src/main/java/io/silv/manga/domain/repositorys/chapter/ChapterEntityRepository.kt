package io.silv.manga.domain.repositorys.chapter

import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.sync.Syncable
import kotlinx.coroutines.flow.Flow

interface ChapterEntityRepository: Syncable {

    val loadingVolumeArtIds: Flow<List<String>>

    suspend fun bookmarkChapter(id: String)

    suspend fun updateLastReadPage(chapterId: String, page: Int, lastPage: Int)

    fun getChapters(mangaId: String): Flow<List<ChapterEntity>>

    fun getAllChapters(): Flow<List<ChapterEntity>>
}

