package io.silv.data.manga

import io.silv.common.model.Resource
import io.silv.database.entity.manga.SourceMangaResource
import kotlinx.coroutines.flow.Flow

interface FilteredYearlyMangaRepository {

    suspend fun refresh()

    fun collectYearlyTopByTagId(tagId: String): Flow<Resource<List<SourceMangaResource>>>
}