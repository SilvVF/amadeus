package io.silv.manga.repositorys.manga

import io.silv.manga.local.entity.manga_resource.FilteredMangaYearlyResource
import io.silv.manga.repositorys.MangaResourceRepository
import kotlinx.coroutines.flow.Flow

interface FilteredYearlyMangaRepository: MangaResourceRepository<FilteredMangaYearlyResource> {
    fun getYearlyTopResources(
        tag: String
    ): Flow<List<FilteredMangaYearlyResource>>
}