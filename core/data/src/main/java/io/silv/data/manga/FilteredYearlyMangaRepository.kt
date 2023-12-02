package io.silv.data.manga

import io.silv.common.model.Resource
import io.silv.database.entity.manga.SourceMangaResource
import kotlinx.coroutines.flow.Flow

interface FilteredYearlyMangaRepository {

    fun getYearlyTopMangaByTagId(tagId: String): Flow<Resource<List<SourceMangaResource>>>
}