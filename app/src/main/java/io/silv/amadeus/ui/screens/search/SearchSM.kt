package io.silv.amadeus.ui.screens.search

import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.models.DomainTag
import io.silv.manga.domain.repositorys.base.LoadState
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.SearchMangaRepository
import io.silv.manga.domain.repositorys.SearchMangaResourceQuery
import io.silv.manga.domain.repositorys.tags.TagRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchSM(
    private val searchMangaRepository: SearchMangaRepository,
    tagRepository: TagRepository,
    private val savedMangaRepository: SavedMangaRepository,
): AmadeusScreenModel<SearchEvent>() {

    private val mutableSearchText = MutableStateFlow("")
    val searchText = mutableSearchText.asStateFlow()

    private val mutableIncludedIds = MutableStateFlow(emptyList<String>())
    val includedIds = mutableIncludedIds.asStateFlow()

    private val mutableExcludedIds = MutableStateFlow(emptyList<String>())
    val excludedIds = mutableExcludedIds.asStateFlow()

    private val loadState = searchMangaRepository.loadState.stateInUi(LoadState.None)

    val tagsUiState = tagRepository.allTags().map {
        it.map { entity -> DomainTag(entity) }
    }
        .stateInUi(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val mangaSearchFlow = combine(
        mutableIncludedIds,
        mutableExcludedIds,
        searchText,
    ) { included, excluded, text  ->
        SearchMangaResourceQuery(
            title = text.ifEmpty { null },
            includedTags = included.ifEmpty { null },
            excludedTags = excluded.ifEmpty { null }
        )
    }
        .debounce(3000)
        .flatMapMerge {
            searchMangaRepository.getMangaResources(it)
        }
        .onStart {
            // emits initial search results prefetched for no query
            // this will avoid the initial result being debounced by 3 seconds
            emit(
                searchMangaRepository
                    .getMangaResources(
                        SearchMangaResourceQuery()
                    ).first()
            )
        }

    val searchMangaUiState = combine(
        mangaSearchFlow,
        loadState,
        savedMangaRepository.getSavedMangas(),
    ) { resources, load, saved ->
        val combinedManga = resources.map {
            DomainManga(it, saved.find { s -> s.id ==  it.id})
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

    fun bookmarkManga(id: String) = coroutineScope.launch {
        savedMangaRepository.bookmarkManga(id)
    }

    fun searchTextChanged(query: String)  {
        mutableSearchText.update { query }
    }

    fun includeTagSelected(id: String) = coroutineScope.launch {
        mutableIncludedIds.update { ids ->
            if (id in ids) {
                ids.filter { it != id    }
            } else {
                ids + id
            }
        }
    }
    fun excludeTagSelected(id: String) = coroutineScope.launch {
        mutableExcludedIds.update { ids ->
            if (id in ids) {
                ids.filter { it != id }
            } else {
                ids + id
            }
        }
    }
}
