@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package io.silv.amadeus.ui.screens.search

import android.util.Log
import androidx.paging.cachedIn
import androidx.paging.map
import cafe.adriel.voyager.core.model.coroutineScope
import com.zhuinden.flowcombinetuplekt.combineTuple
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.amadeus.ui.shared.Language
import io.silv.manga.domain.models.DomainAuthor
import io.silv.manga.domain.models.DomainTag
import io.silv.manga.domain.models.SavableManga
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.SearchMangaRepository
import io.silv.manga.domain.repositorys.SearchMangaResourceQuery
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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

    private val mutableSearchText = MutableStateFlow( "")
    val searchText = mutableSearchText.asStateFlow()

    private val mutableIncludedIds = MutableStateFlow(emptyList<String>())
    val includedIds = mutableIncludedIds.asStateFlow()

    private val mutableExcludedIds = MutableStateFlow( emptyList<String>())
    val excludedIds = mutableExcludedIds.asStateFlow()

    private val mutableIncludedTagMode = MutableStateFlow(MangaRequest.TagsMode.AND)
    val includedTagsMode = mutableIncludedTagMode

    private val mutableExcludedTagMode = MutableStateFlow(MangaRequest.TagsMode.OR)
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

    private var start: Boolean = false
    private val startFlow = MutableStateFlow(false)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
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
            SearchMangaResourceQuery(
                title = text,
                includedTags = included,
                excludedTags = excluded,
                includedTagsMode = includedTagsMode,
                excludedTagsMode = excludedTagsMode,
                authorIds = authors.map { it.id },
                artistIds = artists.map { it.id },
                contentRating = rating,
                publicationStatus = status,
                originalLanguages = originalLangs.map { it.code },
                translatedLanguages = transLangs.map { it.code },
                demographics = demographics
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


    @OptIn(ExperimentalCoroutinesApi::class)
    private val searchManga = searchResourceQuery.flatMapLatest {
        Log.d("SEARCH PAGER", "flatmapLatest")
        searchMangaRepository.pager(it).flow
    }
        .cachedIn(coroutineScope)

    val searchMangaPagingFlow = combineTuple(
        searchManga,
        savedMangaRepository.getSavedMangas(),
    ).map { (pagingData, saved) ->
        pagingData.map {
            SavableManga(it, saved.find { s -> s.id == it.id })
        }
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
