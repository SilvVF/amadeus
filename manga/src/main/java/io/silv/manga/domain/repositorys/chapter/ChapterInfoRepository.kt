package io.silv.manga.domain.repositorys.chapter

import io.silv.manga.domain.repositorys.base.LoadState
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.sync.Syncable
import kotlinx.coroutines.flow.Flow

interface ChapterInfoRepository: Syncable {

    val loadState: Flow<LoadState>

    fun getChapters(mangaId: String): Flow<List<ChapterEntity>>
}

