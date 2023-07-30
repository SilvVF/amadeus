package io.silv.amadeus.ui.screens.home

import cafe.adriel.voyager.core.model.coroutineScope
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.manga.domain.models.DomainManga
import io.silv.manga.domain.models.DomainTag
import io.silv.manga.domain.repositorys.PopularMangaRepository
import io.silv.manga.domain.repositorys.RecentMangaRepository
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.SearchMangaRepository
import io.silv.manga.domain.repositorys.SeasonalMangaRepository
import io.silv.manga.domain.repositorys.TagRepository
import io.silv.manga.domain.repositorys.toBool
import io.silv.manga.local.entity.Season
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HomeSM(
    private val recentMangaRepository: RecentMangaRepository,
    private val popularMangaRepository: PopularMangaRepository,
    seasonalMangaRepository: SeasonalMangaRepository,
    private val savedMangaRepository: SavedMangaRepository,
    searchMangaRepository: SearchMangaRepository,
    private val tagRepository: TagRepository
): AmadeusScreenModel<HomeEvent>() {

    val loadingPopularManga = popularMangaRepository.loadState
        .map(::toBool)
        .stateInUi(false)

    val loadingRecentManga = recentMangaRepository.loadState
        .map(::toBool)
        .stateInUi(false)


    val tagsUiState = tagRepository.allTags().map {
        it.map { tag ->
            DomainTag(tag)
        }
    }
        .stateInUi(emptyList())

    val popularMangaUiState = combine(
        popularMangaRepository.getMangaResources(),
        savedMangaRepository.getSavedMangas()
    ) { resources, saved ->
        resources.map {
            DomainManga(it, saved.find { manga -> manga.id == it.id })
        }
    }
        .stateInUi(emptyList())

    val recentMangaUiState = combine(
        recentMangaRepository.getMangaResources(),
        savedMangaRepository.getSavedMangas()
    ) { resources, saved ->
        resources.map {
            DomainManga(it, saved.find { manga -> manga.id == it.id })
        }
    }
        .stateInUi(emptyList())

    val seasonalMangaUiState = combine(
        seasonalMangaRepository.getSeasonalLists(),
        savedMangaRepository.getSavedMangas()
    ) { seasonWithManga, saved ->
        val yearLists = seasonWithManga.map {
            SeasonalList(
                id = it.list.id,
                year = it.list.year,
                season = it.list.season,
                mangas = it.manga.map { m -> DomainManga(m, saved.find { s -> s.id == m.id }) }
            )
        }
            .sortedBy { it.year * 10000 + it.season.ordinal }
            .takeIf { it.size >= 4 }
            ?.takeLast(4)
            ?: return@combine   SeasonalMangaUiState(
                emptyList()
            )
        SeasonalMangaUiState(
            seasonalLists = yearLists
        )
    }
        .stateInUi(SeasonalMangaUiState(emptyList()))

    fun bookmarkManga(mangaId: String) = coroutineScope.launch {
        savedMangaRepository.bookmarkManga(mangaId)
    }

    fun loadNextPopularPage() = coroutineScope.launch {
        popularMangaRepository.loadNextPage()
    }

    fun loadNextRecentPage() = coroutineScope.launch {
        recentMangaRepository.loadNextPage()
    }
}


data class SeasonalMangaUiState(
    val seasonalLists: List<SeasonalList> = emptyList()
)

data class SeasonalList(
    val id: String,
    val year: Int,
    val season: Season,
    val mangas: List<DomainManga>
)
