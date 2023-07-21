package io.silv.manga.domain.repositorys

import io.silv.manga.local.entity.PopularMangaResource
import kotlinx.coroutines.flow.Flow

interface PopularMangaRepository  {

    fun getMangaResource(id: String): Flow<PopularMangaResource?>

    fun getMangaResources(): Flow<List<PopularMangaResource>>

    suspend fun loadNextPage(): Boolean
}

