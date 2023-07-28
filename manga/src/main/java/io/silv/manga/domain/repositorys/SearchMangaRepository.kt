package io.silv.manga.domain.repositorys

import io.silv.manga.local.entity.SearchMangaResource
import kotlinx.coroutines.flow.Flow

interface SearchMangaRepository  {

    fun getMangaResource(id: String): Flow<SearchMangaResource?>

    fun getMangaResources(query: ResourceQuery): Flow<List<SearchMangaResource>>

    val id: Int
        get() = 3
}

data class ResourceQuery(
    val title: String? = null,
    val includedTags: List<String>? = null,
    val excludedTags: List<String>? = null
)


