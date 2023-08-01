package io.silv.manga.domain.repositorys.base

import io.silv.manga.local.entity.MangaResource
import kotlinx.coroutines.flow.Flow

interface MangaResourceRepository<ResourceType: MangaResource> {

    fun getMangaResource(id: String): Flow<ResourceType?>

    fun getAllMangaResources(): Flow<List<ResourceType>>

    suspend fun refresh()

    val loadState: Flow<LoadState>
}

