package io.silv.manga.domain.repositorys

import io.silv.manga.local.entity.SeasonalMangaResource
import kotlinx.coroutines.flow.Flow


interface SeasonalMangaRepository {

    val loading: Flow<Boolean>

    suspend fun refreshList()

    fun getMangaResource(id: String): Flow<SeasonalMangaResource?>

    fun getMangaResources(): Flow<List<SeasonalMangaResource>>
}
