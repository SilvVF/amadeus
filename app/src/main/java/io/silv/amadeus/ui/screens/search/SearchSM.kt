@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package io.silv.amadeus.ui.screens.search

import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.amadeus.ui.shared.Language
import io.silv.core.combineTuple
import io.silv.manga.domain.models.DomainAuthor
import io.silv.manga.domain.models.DomainTag
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.SearchMangaRepository
import io.silv.manga.domain.repositorys.SearchMangaResourceQuery
import io.silv.manga.domain.repositorys.base.LoadState
import io.silv.manga.domain.repositorys.people.ArtistListRepository
import io.silv.manga.domain.repositorys.people.AuthorListRepository
import io.silv.manga.domain.repositorys.people.QueryResult
import io.silv.manga.domain.repositorys.tags.TagRepository
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.PublicationDemographic
import io.silv.manga.network.mangadex.models.Status
import io.silv.manga.network.mangadex.requests.MangaRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchSM(
    private val searchMangaRepository: SearchMangaRepository,
    tagRepository: TagRepository,
    private val savedMangaRepository: SavedMangaRepository,
    private val authorListRepository: AuthorListRepository,
    private val artistListRepository: ArtistListRepository
): AmadeusScreenModel<SearchEvent>() {

    val status = Status.values().toList()

    private val mutableSelectedDemographics = MutableStateFlow(emptyList<PublicationDemographic>())
    val selectedDemographics = mutableSelectedDemographics.asStateFlow()

    private val mutableSelectedStatus = MutableStateFlow(emptyList<Status>())
    val selectedStatus = mutableSelectedStatus.asStateFlow()

    val contentRatings = ContentRating.values().toList()

    private val mutableSelectedOrigLangs = MutableStateFlow(emptyList<Language>())
    val selectedOrigLangs = mutableSelectedOrigLangs.asStateFlow()

    private val mutableSelectedTransLangs = MutableStateFlow(emptyList<Language>())
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

    private val mutableSearchText = MutableStateFlow("")
    val searchText = mutableSearchText.asStateFlow()

    private val mutableIncludedIds = MutableStateFlow(emptyList<String>())
    val includedIds = mutableIncludedIds.asStateFlow()

    private val mutableExcludedIds = MutableStateFlow(emptyList<String>())
    val excludedIds = mutableExcludedIds.asStateFlow()

    private val mutableIncludedTagMode = MutableStateFlow(MangaRequest.TagsMode.AND)
    val includedTagsMode = mutableIncludedTagMode

    private val mutableExcludedTagMode = MutableStateFlow(MangaRequest.TagsMode.OR)
    val excludedTagsMode = mutableExcludedTagMode

    private val loadState = searchMangaRepository.loadState.stateInUi(LoadState.None)

    val tagsUiState = tagRepository.allTags().map {
        it.map { entity -> DomainTag(entity) }
    }
        .stateInUi(emptyList())

    private var start: Boolean = false
    private val startFlow = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val authorListUiState = mutableAuthorQuery
        .debounce {
            if (it.isNotBlank()) 800L else 0L
        }
        .flatMapLatest { query ->
            authorListRepository.getAuthorList(query)
        }
        .stateInUi(QueryResult.Done(emptyList()))

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val artistListUiState = mutableArtistQuery
        .debounce {
            if (it.isNotBlank()) 800L else 0L
        }
        .flatMapLatest { query ->
            artistListRepository.getArtistList(query)
        }
        .stateInUi(QueryResult.Done(emptyList()))

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val mangaSearchFlow = combineTuple(
        mutableFiltering,
        mutableIncludedIds,
        mutableExcludedIds,
        searchText,
        mutableExcludedTagMode,
        mutableIncludedTagMode,
        selectedAuthors,
        selectedArtists,
        mutableSelectedContentRatings,
        mutableSelectedStatus,
        mutableSelectedOrigLangs,
        mutableSelectedTransLangs,
        mutableSelectedDemographics,
        startFlow,
    ).map { (open, included, excluded, text, includedTagsMode, excludedTagsMode, authors, artists, rating, status, originalLangs, transLangs, demographics,_)  ->
        Pair(
            SearchMangaResourceQuery(
                title = text.ifEmpty { null },
                includedTags = included.ifEmpty { null },
                excludedTags = excluded.ifEmpty { null },
                includedTagsMode = includedTagsMode,
                excludedTagsMode = excludedTagsMode,
                authorIds = authors.map { it.id }.ifEmpty { null },
                artistIds = artists.map { it.id }.ifEmpty { null },
                contentRating = rating.ifEmpty { null },
                publicationStatus = status.ifEmpty { null },
                originalLanguages = originalLangs.map { it.code }.ifEmpty { null },
                translatedLanguages = transLangs.map { it.code }.ifEmpty { null },
                demographics = demographics.ifEmpty { null  }
            ), open
        )
    }
        .dropWhile { (_, menuOpen) -> menuOpen && !start }
        .debounce {
            if (start.also { start = false }) {
                0L
            } else
                2000L
        }
        .flatMapLatest { (query, _) ->
            searchMangaRepository.observeMangaResources(query)
        }
        .onStart {
            // emits initial search results prefetched for no query
            // this will avoid the initial result being debounced by 3 seconds
            emit(
                searchMangaRepository.observeMangaResources(searchMangaRepository.latestQuery())
                    .first()
            )
        }

    val searchMangaUiState = combine(
        mangaSearchFlow,
        loadState,
        savedMangaRepository.getSavedMangas(),
    ) { resources, load, saved ->
        val combinedManga = resources.map {
            SavableManga(it, saved.find { s -> s.id ==  it.id})
        }
        when (load) {
            LoadState.End -> {
                SearchMangaUiState.Success.EndOfPagination(
                    results = combinedManga,
                )
            }
            LoadState.Loading -> {
                SearchMangaUiState.Success.Loading(
                    results = combinedManga,
                )
            }
            LoadState.None -> SearchMangaUiState.Success.Idle(
                results = combinedManga,
            )
            LoadState.Refreshing -> SearchMangaUiState.Refreshing
        }
    }
        .stateInUi(SearchMangaUiState.WaitingForQuery)

    fun loadNextSearchPage() = coroutineScope.launch {
        searchMangaRepository.loadNextPage()
    }

    fun selectDemographic(demographic: PublicationDemographic) = coroutineScope.launch {
        mutableSelectedDemographics.update {
            if (demographic in it) { it - demographic }
            else it + demographic
        }
    }

    fun selectTranslatedLanguage(language: Language) = coroutineScope.launch {
        mutableSelectedTransLangs.update {
            if (language in it) { it - language }
            else it + language
        }
    }

    fun selectOriginalLanguage(language: Language) = coroutineScope.launch {
        mutableSelectedOrigLangs.update {
            if (language in it) { it - language }
            else it + language
        }
    }

    fun isFiltering(bool: Boolean) {
        mutableFiltering.update { bool }
    }

    fun statusSelected(status: Status) = coroutineScope.launch {
        mutableSelectedStatus.update {
            if (status in it) { it - status }
            else it + status
        }
    }

    fun contentRatingSelected(rating: ContentRating) = coroutineScope.launch {
        mutableSelectedContentRatings.update {
            if (rating in it) { it - rating }
            else it + rating
        }
    }

    fun startSearch() = coroutineScope.launch {
        start = true
        startFlow.emit(true)
    }

    fun authorQueryChange(query: String) {
        mutableAuthorQuery.update { query }
    }

    fun authorSelected(author: DomainAuthor) = coroutineScope.launch {
        mutableSelectedAuthors.update {
            if (author in it) it - author
            else it + author
        }
    }

    fun artistQueryChanged(query: String) {
        mutableArtistQuery.update { query }
    }

    fun artistSelected(artist: DomainAuthor) = coroutineScope.launch {
        mutableSelectedArtists.update {
            if (artist in it) it - artist
            else it + artist
        }
    }

    fun includedTagModeChange(mode: MangaRequest.TagsMode) = coroutineScope.launch {
        mutableIncludedTagMode.update { mode }
    }

    fun excludedTagModeChange(mode: MangaRequest.TagsMode) = coroutineScope.launch {
        mutableExcludedTagMode.update { mode }
    }

    fun bookmarkManga(id: String) = coroutineScope.launch {
        savedMangaRepository.bookmarkManga(id)
    }

    fun searchTextChanged(query: String)  {
        mutableSearchText.update { query }
    }

    fun includeTagSelected(id: String) = coroutineScope.launch {
        mutableIncludedIds.update { ids ->
            if (id in ids) ids - id
            else { ids + id }
        }
    }
    fun excludeTagSelected(id: String) = coroutineScope.launch {
        mutableExcludedIds.update { ids ->
            if (id in ids) ids - id
            else { ids + id}
        }
    }
}
