package io.silv.manga.domain.repositorys

import io.silv.manga.local.entity.RecentMangaResource
import kotlinx.coroutines.flow.Flow

interface RecentMangaRepository  {

    fun getMangaResource(id: String): Flow<RecentMangaResource?>

    fun getMangaResources(): Flow<List<RecentMangaResource>>

    suspend fun loadNextPage(): Boolean
}
