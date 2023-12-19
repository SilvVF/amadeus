package io.silv.domain.manga

import io.silv.data.manga.MangaRepository
import io.silv.data.manga.SeasonalMangaRepository
import io.silv.model.DomainSeasonalList
import io.silv.model.SavableManga
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map

class SubscribeToSeasonalLists(
    private val seasonalRepository: SeasonalMangaRepository,
    private val mangaRepository: MangaRepository,
) {
    fun subscribe() =
        seasonalRepository.getSeasonalLists().map { lists ->
            lists.map { (list, mangas) ->
                DomainSeasonalList(
                    id = list.id,
                    season = list.season,
                    year = list.year,
                    mangas = mangas.map { SavableManga(it) }.toImmutableList()
                )
            }
        }
}
