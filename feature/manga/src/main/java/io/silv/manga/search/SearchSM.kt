@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package io.silv.manga.search

import android.util.Log
import androidx.paging.PagingConfig
import cafe.adriel.voyager.core.model.screenModelScope
import com.zhuinden.flowcombinetuplekt.combineTuple
import io.silv.common.model.ContentRating
import io.silv.common.model.PagedType
import io.silv.common.model.PublicationDemographic
import io.silv.common.model.QueryFilters
import io.silv.common.model.QueryResult
import io.silv.common.model.Status
import io.silv.common.model.TagsMode
import io.silv.data.author.AuthorListRepository
import io.silv.data.manga.SavedMangaRepository
import io.silv.data.tags.TagRepository
import io.silv.domain.SubscribeToPagingData
import io.silv.model.DomainAuthor
import io.silv.model.DomainTag
import io.silv.ui.EventScreenModel
import io.silv.ui.ioCoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchSM(
    subscribeToPagingData: SubscribeToPagingData,
    tagRepository: TagRepository,
    private val savedMangaRepository: SavedMangaRepository,
    private val authorListRepository: AuthorListRepository,
): EventScreenModel<SearchEvent>() {

    val status = Status.values().toList()

    private val mutableSelectedDemographics = MutableStateFlow(emptyList<PublicationDemographic>())
    val selectedDemographics = mutableSelectedDemographics.asStateFlow()

    private val mutableSelectedStatus = MutableStateFlow(emptyList<Status>())
    val selectedStatus = mutableSelectedStatus.asStateFlow()

    val contentRatings = ContentRating.values().toList()

    private val mutableSelectedOrigLangs = MutableStateFlow(emptyList<io.silv.ui.Language>())
    val selectedOrigLangs = mutableSelectedOrigLangs.asStateFlow()

    private val mutableSelectedTransLangs = MutableStateFlow(emptyList<io.silv.ui.Language>())
    val selectedTransLang = mutableSelectedTransLangs.asStateFlow()

    private val mutableSelectedContentRatings = MutableStateFlow(emptyList<ContentRating>())
    val selectedContentRatings = mutableSelectedContentRatings.asStateFlow()

    private val mutableAuthorQuery = MutableStateFlow("")
    val authorQuery = mutableAuthorQuery.asStateFlow()

    private val mutableSelectedAuthors = MutableStateFlow(emptyList<DomainAuthor>())
    val selectedAuthors = mutableSelectedAuthors.asStateFlow()

    private val mutableArtistQuery = MutableStateFlow("")
    val artistQuery = mutableArtistQuery.asStateFlow()

    private val mutableSelectedArtists = MutableStateFlow(emptyList<DomainAuthor>())
    val selectedArtists = mutableSelectedArtists.asStateFlow()

    private val mutableFiltering = MutableStateFlow(false)
    val filtering = mutableFiltering.asStateFlow()

    private val mutableSearchText = MutableStateFlow( "")
    val searchText = mutableSearchText.asStateFlow()

    private val mutableIncludedIds = MutableStateFlow(emptyList<String>())
    val includedIds = mutableIncludedIds.asStateFlow()

    private val mutableExcludedIds = MutableStateFlow( emptyList<String>())
    val excludedIds = mutableExcludedIds.asStateFlow()

    private val mutableIncludedTagMode = MutableStateFlow(TagsMode.AND)
    val includedTagsMode = mutableIncludedTagMode

    private val mutableExcludedTagMode = MutableStateFlow(TagsMode.OR)
    val excludedTagsMode = mutableExcludedTagMode

    val tagsUiState = tagRepository.allTags().map {
        it.map { entity -> DomainTag(entity) }
    }
        .stateInUi(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val authorListUiState = mutableAuthorQuery
        .debounce {
            if (it.isNotBlank()) 800L else 0L
        }
        .flatMapLatest { query ->
            authorListRepository.getAuthorList(query).map { response ->
                when (response) {
                    is QueryResult.Done -> QueryResult.Done(
                        response.result.map { author ->
                            DomainAuthor(
                                name = author.attributes.name,
                                id = author.id
                            )
                        }
                    )
                    QueryResult.Loading -> QueryResult.Loading
                }
            }
        }
        .stateInUi(QueryResult.Done(emptyList()))

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val artistListUiState = mutableArtistQuery
        .debounce {
            if (it.isNotBlank()) 800L else 0L
        }
        .flatMapLatest { query ->
            authorListRepository.getAuthorList(query).map { response ->
                when (response) {
                    is QueryResult.Done -> QueryResult.Done(
                        response.result.map { author ->
                            DomainAuthor(
                                name = author.attributes.name,
                                id = author.id
                            )
                        }
                    )
                    QueryResult.Loading -> QueryResult.Loading
                }
            }
        }
        .stateInUi(QueryResult.Done(emptyList()))

    private var start: Boolean = false
    private val startFlow = MutableStateFlow(false)

    @OptIn(FlowPreview::class)
    private val searchResourceQuery = combineTuple(
        mutableFiltering,
        mutableIncludedIds,
        mutableExcludedIds,
        searchText,
        includedTagsMode,
        excludedTagsMode,
        selectedAuthors,
        selectedArtists,
        mutableSelectedContentRatings,
        mutableSelectedStatus,
        mutableSelectedOrigLangs,
        mutableSelectedTransLangs,
        mutableSelectedDemographics,
        startFlow,
    ).map { (open, included, excluded, text, includedTagsMode, excludedTagsMode, authors, artists, rating, status, originalLangs, transLangs, demographics, _)  ->
        Pair(
            PagedType.Query(
                QueryFilters(
                    text
                )
            ),
            open
        )
    }
        .dropWhile { (_, menuOpen) -> (menuOpen && !start).also {   Log.d("SEARCH PAGER", "dropped-$it") } }
        .debounce {
            Log.d("SEARCH PAGER", "debouncing")
            if (start.also { start = false }) { 0L } else 2000L
        }
        .map { it.first }



    val searchMangaPagingFlow = subscribeToPagingData(
        config = PagingConfig(30),
        typeFlow = searchResourceQuery,
        scope = ioCoroutineScope
    )



    fun selectDemographic(demographic: PublicationDemographic) {
        screenModelScope.launch {
            mutableSelectedDemographics.update {
                if (demographic in it) { it - demographic }
                else it + demographic
            }
        }
    }

    fun selectTranslatedLanguage(language: io.silv.ui.Language) {
        screenModelScope.launch {
            mutableSelectedTransLangs.update {
                if (language in it) { it - language }
                else it + language
            }
        }
    }

    fun selectOriginalLanguage(language: io.silv.ui.Language) {
        screenModelScope.launch {
            mutableSelectedOrigLangs.update {
                if (language in it) { it - language }
                else it + language
            }
        }
    }

    fun isFiltering(bool: Boolean) {
        mutableFiltering.update { bool }
    }

    fun statusSelected(status: Status) {
        screenModelScope.launch {
            mutableSelectedStatus.update {
                if (status in it) { it - status }
                else it + status
            }
        }
    }

    fun contentRatingSelected(rating: ContentRating) {
        screenModelScope.launch {
            mutableSelectedContentRatings.update {
                if (rating in it) { it - rating }
                else it + rating
            }
        }
    }

    fun startSearch() {
        screenModelScope.launch {
            start = true
            startFlow.emit(true)
        }
    }

    fun authorQueryChange(query: String) {
        mutableAuthorQuery.update { query }
    }

    fun authorSelected(author: DomainAuthor) {
        screenModelScope.launch {
            mutableSelectedAuthors.update {
                if (author in it) it - author
                else it + author
            }
        }
    }

    fun artistQueryChanged(query: String) {
        mutableArtistQuery.update { query }
    }

    fun artistSelected(artist: DomainAuthor) {
        screenModelScope.launch {
            mutableSelectedArtists.update {
                if (artist in it) it - artist
                else it + artist
            }
        }
    }

    fun includedTagModeChange(mode: TagsMode) {
        screenModelScope.launch {
            mutableIncludedTagMode.update { mode }
        }
    }

    fun excludedTagModeChange(mode: TagsMode) {
        screenModelScope.launch {
            mutableExcludedTagMode.update { mode }
        }
    }

    fun bookmarkManga(id: String) {
        screenModelScope.launch {
            savedMangaRepository.bookmarkManga(id)
        }
    }

    fun searchTextChanged(query: String)  {
        mutableSearchText.update { query }
    }

    fun includeTagSelected(id: String) {
        screenModelScope.launch {
            mutableIncludedIds.update { ids ->
                if (id in ids) ids - id
                else {
                    ids + id
                }
            }
        }
    }
    fun excludeTagSelected(id: String) {
        screenModelScope.launch {
            mutableExcludedIds.update { ids ->
                if (id in ids) ids - id
                else { ids + id}
            }
        }
    }
}
