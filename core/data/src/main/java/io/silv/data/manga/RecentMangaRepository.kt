package io.silv.data.manga

import androidx.paging.Pager
import io.silv.database.entity.manga.resource.RecentMangaResource
import kotlinx.coroutines.flow.Flow

interface RecentMangaRepository {

    val pager: Pager<Int, RecentMangaResource>

    fun observeMangaResourceById(id: String): Flow<RecentMangaResource?>

    fun observeAllMangaResources(): Flow<List<RecentMangaResource>>
}

