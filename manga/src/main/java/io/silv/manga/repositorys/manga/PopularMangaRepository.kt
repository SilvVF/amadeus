package io.silv.manga.repositorys.manga

import androidx.paging.Pager
import io.silv.manga.local.entity.manga_resource.PopularMangaResource
import kotlinx.coroutines.flow.Flow

interface PopularMangaRepository {

    val pager: Pager<Int, PopularMangaResource>

    fun observeMangaResourceById(id: String): Flow<PopularMangaResource?>

    fun observeAllMangaResources(): Flow<List<PopularMangaResource>>
}

