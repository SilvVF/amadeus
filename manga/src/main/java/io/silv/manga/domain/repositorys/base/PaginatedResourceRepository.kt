package io.silv.manga.domain.repositorys.base

import io.silv.manga.local.entity.MangaResource
import kotlinx.coroutines.flow.Flow

interface PaginatedResourceRepository<ResourceType: MangaResource, ResourceQuery>:
    MangaResourceRepository<ResourceType> {

    fun latestQuery(): ResourceQuery

    fun observeMangaResources(resourceQuery: ResourceQuery): Flow<List<ResourceType>>

    suspend fun loadNextPage()
}
