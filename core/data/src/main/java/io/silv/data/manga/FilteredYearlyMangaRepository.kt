package io.silv.data.manga

import io.silv.common.model.LoadState
import io.silv.database.entity.manga.SourceMangaResource
import kotlinx.coroutines.flow.Flow

interface FilteredYearlyMangaRepository {

    suspend fun refresh()

    val loadState: Flow<LoadState>

    fun getYearlyTopResources(
        tag: String
    ): Flow<List<SourceMangaResource>>
}