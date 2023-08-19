package io.silv.manga.repositorys.manga

import androidx.paging.Pager
import io.silv.manga.local.entity.manga_resource.FilteredMangaResource
import kotlinx.coroutines.flow.Flow

interface FilteredMangaRepository {

    fun pager(query: FilteredResourceQuery): Pager<Int, FilteredMangaResource>

    fun observeMangaResourceById(id: String): Flow<FilteredMangaResource?>

    fun observeAllMangaResources(): Flow<List<FilteredMangaResource>>

    enum class TimePeriod {
        SixMonths, ThreeMonths, LastMonth, OneWeek, AllTime
    }
}
