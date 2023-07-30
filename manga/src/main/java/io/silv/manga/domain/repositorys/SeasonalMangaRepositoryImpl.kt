package io.silv.manga.domain.repositorys

import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.MangaToSeasonalMangaResourceMapper
import io.silv.manga.local.dao.SeasonalListDao
import io.silv.manga.local.dao.SeasonalMangaResourceDao
import io.silv.manga.local.entity.Season
import io.silv.manga.local.entity.SeasonalListEntity
import io.silv.manga.local.entity.SeasonalMangaResource
import io.silv.manga.local.entity.relations.SeasonListWithManga
import io.silv.manga.local.entity.syncerForEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.network.mangadex.requests.MangaRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal class SeasonalMangaRepositoryImpl(
    private val mangaResourceDao: SeasonalMangaResourceDao,
    private val seasonalListDao: SeasonalListDao,
    private val mangaDexApi: MangaDexApi,
    private val dispatchers: AmadeusDispatchers
): SeasonalMangaRepository {

    private fun String.toSeason() = when {
        this.contains("winter", ignoreCase = true) -> Season.Winter
        this.contains("summer", ignoreCase = true) -> Season.Summer
        this.contains("fall", ignoreCase = true) -> Season.Fall
        this.contains("spring", ignoreCase = true) -> Season.Spring
        else -> null
    }

    override suspend fun updateSeasonList(season: Season, year: Int): Unit = withContext(dispatchers.io) {
        runCatching {
            if (seasonalListDao.getAll().any { it.year == year  && it.season == season}) {
                return@withContext
            }

            val mangaDexLists = mangaDexApi
                .getUserLists("d2ae45e0-b5e2-4e7f-a688-17925c2d7d6b")
                .getOrThrow()
                .data

            mangaDexLists.mapNotNull {
                Triple(
                    it.attributes.name.toSeason() ?: return@mapNotNull null,
                    it.attributes.name.filter { c -> c.isDigit() }.toIntOrNull() ?: return@mapNotNull null,
                    it
                )
            }.filter { (s, y, _) ->
                season == s && year == y
            }.forEach { (s, y , list) ->
                val response = mangaDexApi.getMangaList(
                    MangaRequest(
                        limit = 100,
                        ids = list.relationships
                            .filter { it.type == "manga" }
                            .map { it.id },
                        includes = listOf("cover_art"),
                        order = mapOf("followedCount" to "desc"),
                        hasAvailableChapters = true
                    )
                )
                    .getOrThrow()
                    .data

                seasonalListDao.upsertList(
                    SeasonalListEntity(
                        id = list.id,
                        year = y,
                        season = s,
                    )
                )
                val result = syncerForEntity<SeasonalMangaResource, Manga, String>(
                    networkToKey = { it.id },
                    mapper = { manga, saved ->
                        MangaToSeasonalMangaResourceMapper.map(manga to saved).copy(
                            seasonId = list.id
                        )
                    },
                    upsert = {
                        mangaResourceDao.upsertManga(it)
                    }
                ).sync(
                    current = mangaResourceDao.getAll().filter { it.seasonId == list.id },
                    networkResponse = response
                )
                for (unhandled in result.unhandled) {
                    mangaResourceDao.delete(unhandled)
                }
            }
        }
            .onFailure { it.printStackTrace() }
    }

    override fun getSeasonalManga(id: String): Flow<SeasonalMangaResource?> {
        return mangaResourceDao.getResourceAsFlowById(id)
    }

    override fun getSeasonalLists(): Flow<List<SeasonListWithManga>> {
        val year = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
        return seasonalListDao.getSeasonListWithManga()
            .onStart {
                CoroutineScope(dispatchers.io).launch {
                    Season.values().forEach {
                        updateSeasonList(
                            season = it,
                            year = year
                        )
                        updateSeasonList(
                            season = it,
                            year = year - 1
                        )
                    }
                }
            }
    }
}