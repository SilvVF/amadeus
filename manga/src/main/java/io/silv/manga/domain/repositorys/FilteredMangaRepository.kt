package io.silv.manga.domain.repositorys

import io.silv.manga.local.entity.FilteredMangaResource
import kotlinx.coroutines.flow.Flow

interface FilteredMangaRepository {

    fun getMangaResource(id: String): Flow<FilteredMangaResource?>

    fun getMangaResources(
        tag: String,
    ): Flow<List<FilteredMangaResource>>

    suspend fun loadNextPage(): Boolean
}