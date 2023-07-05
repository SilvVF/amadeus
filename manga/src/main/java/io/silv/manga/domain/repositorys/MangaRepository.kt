package io.silv.manga.domain.repositorys

import io.silv.manga.local.entity.MangaResource
import io.silv.manga.sync.Syncable
import kotlinx.coroutines.flow.Flow


interface MangaRepository  {
    /**
     * Returns available news resources that match the specified [query].
     */
    fun getMagnaResources(query: MangaQuery): Flow<List<MangaResource>>

    suspend fun loadNextPage(): Boolean
}





