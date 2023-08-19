package io.silv.manga.repositorys

import io.silv.manga.local.entity.MangaResource
import kotlinx.coroutines.flow.Flow

interface MangaResourceRepository<ResourceType: MangaResource> {

    fun observeMangaResourceById(id: String): Flow<ResourceType?>

    fun observeAllMangaResources(): Flow<List<ResourceType>>

    suspend fun refresh()

    val loadState: Flow<LoadState>
}