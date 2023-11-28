package io.silv.data.manga

import androidx.paging.Pager
import io.silv.database.entity.manga.resource.QuickSearchMangaResource
import kotlinx.coroutines.flow.Flow

interface QuickSearchMangaRepository {

    fun pager(query: String): Pager<Int, QuickSearchMangaResource>

    fun observeMangaResourceById(id: String): Flow<QuickSearchMangaResource?>

    fun observeAllMangaResources(): Flow<List<QuickSearchMangaResource>>
}


