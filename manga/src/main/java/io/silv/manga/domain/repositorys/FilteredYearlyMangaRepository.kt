package io.silv.manga.domain.repositorys

import io.silv.manga.domain.repositorys.base.MangaResourceRepository
import io.silv.manga.local.entity.FilteredMangaYearlyResource
import kotlinx.coroutines.flow.Flow

interface FilteredYearlyMangaRepository: MangaResourceRepository<FilteredMangaYearlyResource> {
    fun getYearlyTopResources(
        tag: String
    ): Flow<List<FilteredMangaYearlyResource>>
}