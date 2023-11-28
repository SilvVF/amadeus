package io.silv.data.manga

import androidx.paging.Pager
import io.silv.common.model.ContentRating
import io.silv.common.model.PublicationDemographic
import io.silv.common.model.Status
import io.silv.database.entity.manga.resource.SearchMangaResource
import io.silv.network.requests.MangaRequest
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
    val contentRating: List<ContentRating> = listOf(ContentRating.safe),
    val publicationStatus: List<Status> = emptyList(),
    val authorIds: List<String> = emptyList(),
    val artistIds: List<String> = emptyList(),
    val originalLanguages: List<String> = emptyList(),
    val translatedLanguages: List<String> = emptyList(),
    val demographics: List<PublicationDemographic> = emptyList()
)
