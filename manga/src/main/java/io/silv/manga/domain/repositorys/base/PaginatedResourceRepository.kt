package io.silv.manga.domain.repositorys.base

import io.silv.manga.local.entity.MangaResource
import kotlinx.coroutines.flow.Flow

interface PaginatedResourceRepository<ResourceType: MangaResource, ResourceQuery> {

    fun observeMangaResourceById(id: String): Flow<ResourceType?>

    fun observeAllMangaResources(): Flow<List<ResourceType>>

    suspend fun refresh(resourceQuery: ResourceQuery? = null)

    val loadState: Flow<PagedLoadState>

    fun latestQuery(): ResourceQuery

    fun observeMangaResources(resourceQuery: ResourceQuery): Flow<List<ResourceType>>

    suspend fun loadNextPage()
}
