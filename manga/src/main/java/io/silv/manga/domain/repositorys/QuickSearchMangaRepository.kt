package io.silv.manga.domain.repositorys

import androidx.paging.Pager
import io.silv.manga.local.entity.QuickSearchMangaResource
import kotlinx.coroutines.flow.Flow

interface QuickSearchMangaRepository {

    fun pager(query: String): Pager<Int, QuickSearchMangaResource>

    fun observeMangaResourceById(id: String): Flow<QuickSearchMangaResource?>

    fun observeAllMangaResources(): Flow<List<QuickSearchMangaResource>>
}


