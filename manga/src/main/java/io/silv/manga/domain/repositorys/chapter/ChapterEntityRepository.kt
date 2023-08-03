package io.silv.manga.domain.repositorys.chapter

import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.sync.Syncable
import kotlinx.coroutines.flow.Flow

interface ChapterEntityRepository: Syncable {

    fun getChapters(mangaId: String): Flow<List<ChapterEntity>>
}

