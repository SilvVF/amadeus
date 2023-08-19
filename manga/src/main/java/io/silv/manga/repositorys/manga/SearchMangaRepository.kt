package io.silv.manga.repositorys.manga

import androidx.paging.Pager
import io.silv.manga.local.entity.manga_resource.SearchMangaResource
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.PublicationDemographic
import io.silv.manga.network.mangadex.models.Status
import io.silv.manga.network.mangadex.requests.MangaRequest
import kotlinx.coroutines.flow.Flow

interface SearchMangaRepository {

    fun pager(query: SearchMangaResourceQuery): Pager<Int, SearchMangaResource>

    fun observeMangaResourceById(id: String): Flow<SearchMangaResource?>

    fun observeAllMangaResources(): Flow<List<SearchMangaResource>>
}

data class SearchMangaResourceQuery(
    val title: String = "",
    val includedTags: List<String> = emptyList(),
    val excludedTags: List<String> = emptyList(),
    val includedTagsMode: MangaRequest.TagsMode = MangaRequest.TagsMode.AND,
    val excludedTagsMode: MangaRequest.TagsMode = MangaRequest.TagsMode.OR,
    val contentRating: List<ContentRating> = emptyList(),
    val publicationStatus: List<Status> = emptyList(),
    val authorIds: List<String> = emptyList(),
    val artistIds: List<String> = emptyList(),
    val originalLanguages: List<String> = emptyList(),
    val translatedLanguages: List<String> = emptyList(),
    val demographics: List<PublicationDemographic> = emptyList()
)
