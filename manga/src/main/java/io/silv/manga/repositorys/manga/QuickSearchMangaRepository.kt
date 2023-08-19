package io.silv.manga.repositorys.manga

import androidx.paging.Pager
import io.silv.manga.local.entity.manga_resource.QuickSearchMangaResource
import kotlinx.coroutines.flow.Flow

interface QuickSearchMangaRepository {

    fun pager(query: String): Pager<Int, QuickSearchMangaResource>

    fun observeMangaResourceById(id: String): Flow<QuickSearchMangaResource?>

    fun observeAllMangaResources(): Flow<List<QuickSearchMangaResource>>
}


