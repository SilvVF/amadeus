package io.silv.manga.domain.repositorys

import io.silv.manga.local.entity.SearchMangaResource
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.Status
import io.silv.manga.network.mangadex.requests.MangaRequest
import kotlinx.coroutines.flow.Flow

interface SearchMangaRepository: MangaResourceRepository<SearchMangaResource>  {

    override fun getMangaResource(id: String): Flow<SearchMangaResource?>

    fun getMangaResources(query: ResourceQuery): Flow<List<SearchMangaResource>>

    val id: Int
        get() = 3
}

data class ResourceQuery(
    val title: String? = null,
    val includedTags: List<String>? = null,
    val excludedTags: List<String>? = null,
    val includedTagsMode: MangaRequest.TagsMode,
    val excludedTagsMode: MangaRequest.TagsMode,
    val contentRating: ContentRating?,
    val publicationStatus: List<Status>?
)


