package io.silv.manga.domain.repositorys

import io.silv.manga.domain.repositorys.base.PaginatedResourceRepository
import io.silv.manga.local.entity.SearchMangaResource
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.Status
import io.silv.manga.network.mangadex.requests.MangaRequest

interface SearchMangaRepository: PaginatedResourceRepository<SearchMangaResource, SearchMangaResourceQuery>

data class SearchMangaResourceQuery(
    val title: String? = null,
    val includedTags: List<String>? = null,
    val excludedTags: List<String>? = null,
    val includedTagsMode: MangaRequest.TagsMode? = MangaRequest.TagsMode.AND,
    val excludedTagsMode: MangaRequest.TagsMode? = MangaRequest.TagsMode.OR,
    val contentRating: List<ContentRating>? = null,
    val publicationStatus: List<Status>? = null,
    val authorIds: List<String>? = null,
    val artistIds: List<String>? = null
)


