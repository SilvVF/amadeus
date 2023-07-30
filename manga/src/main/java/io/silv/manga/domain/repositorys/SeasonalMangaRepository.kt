package io.silv.manga.domain.repositorys

import io.silv.manga.local.entity.Season
import io.silv.manga.local.entity.SeasonalMangaResource
import io.silv.manga.local.entity.relations.SeasonListWithManga
import kotlinx.coroutines.flow.Flow


interface SeasonalMangaRepository {

    suspend fun updateSeasonList(season: Season, year: Int)

    fun getSeasonalManga(id: String): Flow<SeasonalMangaResource?>

    fun getSeasonalLists(): Flow<List<SeasonListWithManga>>
}
