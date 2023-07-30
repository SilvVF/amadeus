package io.silv.amadeus.ui.screens.manga_filter

import android.util.Log
import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.repositorys.FilteredMangaRepository
import io.silv.manga.domain.repositorys.SavedMangaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.launch

class MangaFilterSM(
    private val filteredMangaRepository: FilteredMangaRepository,
    private val savedMangaRepository: SavedMangaRepository,
    tagId: String
): AmadeusScreenModel<MangaFilterEvent>() {

    val yearlyFilteredUiState = combine(
        filteredMangaRepository.getYearlyTopResources(tagId),
        savedMangaRepository.getSavedMangas()
    ) { resources, saved ->
        resources.map { r ->
            DomainManga(r, saved.find { it.id == r.id })
        }
    }
        .stateInUi(emptyList())


    private val mutableTimePeriod = MutableStateFlow(FilteredMangaRepository.TimePeriod.AllTime)
    val timePeriod = mutableTimePeriod.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val timePeriodFilteredResources = mutableTimePeriod.flatMapMerge {
        Log.d("FILTER", it.toString() + tagId)
        filteredMangaRepository.getMangaResources(tagId, it)
    }

    val timePeriodFilteredUiState = combine(
        timePeriodFilteredResources,
        savedMangaRepository.getSavedMangas()
    ) { resources, saved ->
        resources.map { r ->
            DomainManga(r, saved.find { it.id == r.id })
        }
    }
        .stateInUi(emptyList())


    fun loadNextPage() = coroutineScope.launch {
        filteredMangaRepository.loadNextPage()
    }

    fun changeTimePeriod(timePeriod: FilteredMangaRepository.TimePeriod) = coroutineScope.launch {
        mutableTimePeriod.emit(timePeriod)
    }

    fun bookmarkManga(id: String) = coroutineScope.launch {
        println("bookmark clicked $id")
        savedMangaRepository.bookmarkManga(id)
    }
}

