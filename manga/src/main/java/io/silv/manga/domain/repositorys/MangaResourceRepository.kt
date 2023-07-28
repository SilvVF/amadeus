package io.silv.manga.domain.repositorys

import io.silv.manga.local.entity.MangaResource
import kotlinx.coroutines.flow.Flow

interface MangaResourceRepository<ResourceType: MangaResource> {

    fun getMangaResource(id: String): Flow<ResourceType?>

    fun getMangaResources(): Flow<List<ResourceType>>

    suspend fun loadNextPage(): Boolean
}