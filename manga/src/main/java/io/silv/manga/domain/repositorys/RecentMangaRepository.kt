package io.silv.manga.domain.repositorys

import androidx.paging.Pager
import io.silv.manga.local.entity.RecentMangaResource
import kotlinx.coroutines.flow.Flow

interface RecentMangaRepository {

    val pager: Pager<Int, RecentMangaResource>

    fun observeMangaResourceById(id: String): Flow<RecentMangaResource?>

    fun observeAllMangaResources(): Flow<List<RecentMangaResource>>
}

