package io.silv.domain

import io.silv.data.manga.SavedMangaRepository
import io.silv.data.manga.SeasonalMangaRepository
import io.silv.model.DomainSeasonalList
import io.silv.model.SavableManga
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SubscribeToSeasonalLists(
    private val seasonalRepository: SeasonalMangaRepository,
    private val savedMangaRepository: SavedMangaRepository
) {

    fun getLists(scope: CoroutineScope) = seasonalRepository.getSeasonalLists().map { lists ->
        lists.map { (list, mangas) ->

            DomainSeasonalList(
                id = list.id,
                season = list.season,
                year = list.year,
                mangas = mangas.map { sourceManga ->
                    savedMangaRepository.getSavedManga(sourceManga.id).map { saved ->
                        SavableManga(sourceManga, saved)
                    }
                        .stateIn(
                            scope,
                            SharingStarted.Lazily,
                            SavableManga(sourceManga, null)
                        )
                }
                    .toImmutableList()
            )
        }
    }
}