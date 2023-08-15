package io.silv.manga.domain.repositorys

import androidx.paging.Pager
import io.silv.manga.local.entity.PopularMangaResource
import kotlinx.coroutines.flow.Flow

interface PopularMangaRepository {

    val pager: Pager<Int, PopularMangaResource>

    fun observeMangaResourceById(id: String): Flow<PopularMangaResource?>

    fun observeAllMangaResources(): Flow<List<PopularMangaResource>>
}

