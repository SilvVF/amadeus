package io.silv.manga.domain.repositorys

import io.silv.manga.domain.repositorys.base.PaginatedResourceRepository
import io.silv.manga.local.entity.FilteredMangaResource

interface FilteredMangaRepository:
    PaginatedResourceRepository<FilteredMangaResource, FilteredResourceQuery?> {

    enum class TimePeriod {
        SixMonths, ThreeMonths, LastMonth, OneWeek, AllTime
    }
}
