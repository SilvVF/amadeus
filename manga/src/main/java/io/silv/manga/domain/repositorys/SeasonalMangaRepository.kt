package io.silv.manga.domain.repositorys

import io.silv.manga.domain.repositorys.base.MangaResourceRepository
import io.silv.manga.local.entity.Season
import io.silv.manga.local.entity.SeasonalMangaResource
import io.silv.manga.local.entity.relations.SeasonListWithManga
import kotlinx.coroutines.flow.Flow


interface SeasonalMangaRepository: MangaResourceRepository<SeasonalMangaResource> {

    suspend fun updateSeasonList(season: Season, year: Int)

    fun getSeasonalLists(): Flow<List<SeasonListWithManga>>
}
