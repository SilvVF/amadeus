package io.silv.data.manga

import androidx.paging.Pager
import io.silv.database.entity.manga.resource.PopularMangaResource
import kotlinx.coroutines.flow.Flow

interface PopularMangaRepository {

    val pager: Pager<Int, PopularMangaResource>

    fun observeMangaResourceById(id: String): Flow<PopularMangaResource?>

    fun observeAllMangaResources(): Flow<List<PopularMangaResource>>
}

