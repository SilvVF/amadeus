package io.silv.data.manga

import io.silv.common.model.LoadState
import io.silv.database.entity.manga.resource.FilteredMangaYearlyResource
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