package io.silv.manga.domain.repositorys

import io.silv.manga.domain.models.DomainChapter
import io.silv.manga.local.entity.ChapterEntity
import io.silv.manga.sync.Syncable
import kotlinx.coroutines.flow.Flow

interface ChapterInfoRepository: Syncable {

    val loading: Flow<Boolean>

    fun getChapters(mangaId: String): Flow<List<ChapterEntity>>
}

