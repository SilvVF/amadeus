package io.silv.manga.domain.repositorys

import io.silv.manga.local.entity.MangaResource
import kotlinx.coroutines.flow.Flow


interface MangaRepository  {

    fun getMangaResource(id: String): Flow<MangaResource>

    fun getMangaResources(query: MangaQuery): Flow<List<MangaResource>>

    suspend fun loadNextPage(): Boolean
}





