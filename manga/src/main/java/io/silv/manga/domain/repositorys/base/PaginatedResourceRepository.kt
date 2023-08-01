package io.silv.manga.domain.repositorys.base

import io.silv.manga.local.entity.MangaResource
import kotlinx.coroutines.flow.Flow

interface PaginatedResourceRepository<ResourceType: MangaResource, ResourceQuery>:
    MangaResourceRepository<ResourceType> {

    fun getMangaResources(resourceQuery: ResourceQuery): Flow<List<ResourceType>>

    suspend fun loadNextPage()
}
