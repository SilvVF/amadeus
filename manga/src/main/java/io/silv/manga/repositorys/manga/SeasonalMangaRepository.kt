package io.silv.manga.repositorys.manga

import io.silv.manga.local.entity.Season
import io.silv.manga.local.entity.manga_resource.SeasonalMangaResource
import io.silv.manga.local.entity.relations.SeasonListWithManga
import io.silv.manga.repositorys.MangaResourceRepository
import kotlinx.coroutines.flow.Flow


interface SeasonalMangaRepository: MangaResourceRepository<SeasonalMangaResource> {

    suspend fun updateSeasonList(season: Season, year: Int)

    fun getSeasonalLists(): Flow<List<SeasonListWithManga>>
}
