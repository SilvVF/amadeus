package io.silv.data.manga

import androidx.paging.Pager
import io.silv.database.entity.manga.resource.FilteredMangaResource
import kotlinx.coroutines.flow.Flow

interface FilteredMangaRepository {

    fun pager(query: FilteredResourceQuery): Pager<Int, FilteredMangaResource>

    fun observeMangaResourceById(id: String): Flow<FilteredMangaResource?>

    fun observeAllMangaResources(): Flow<List<FilteredMangaResource>>
}
