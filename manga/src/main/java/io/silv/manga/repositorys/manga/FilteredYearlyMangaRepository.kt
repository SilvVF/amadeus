package io.silv.manga.repositorys.manga

import io.silv.manga.local.entity.manga_resource.FilteredMangaYearlyResource
import io.silv.manga.repositorys.LoadState
import kotlinx.coroutines.flow.Flow

interface FilteredYearlyMangaRepository {

    fun observeMangaResourceById(id: String): Flow<FilteredMangaYearlyResource?>

    fun observeAllMangaResources(): Flow<List<FilteredMangaYearlyResource>>

    suspend fun refresh()

    val loadState: Flow<LoadState>

    fun getYearlyTopResources(
        tag: String
    ): Flow<List<FilteredMangaYearlyResource>>
}