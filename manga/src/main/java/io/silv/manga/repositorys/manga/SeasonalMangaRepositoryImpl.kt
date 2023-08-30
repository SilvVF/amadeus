package io.silv.manga.repositorys.manga

import androidx.room.withTransaction
import io.silv.core.AmadeusDispatchers
import io.silv.core.pmap
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.local.AmadeusDatabase
import io.silv.manga.local.dao.SeasonalListDao
import io.silv.manga.local.dao.SeasonalMangaResourceDao
import io.silv.manga.local.entity.Season
import io.silv.manga.local.entity.SeasonalListEntity
import io.silv.manga.local.entity.manga_resource.SeasonalMangaResource
import io.silv.manga.local.entity.relations.SeasonListWithManga
import io.silv.manga.local.entity.syncerForEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.list.Data
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.network.mangadex.requests.MangaRequest
import io.silv.manga.repositorys.suspendRunCatching
import io.silv.manga.repositorys.toSeasonalMangaResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext


internal class SeasonalMangaRepositoryImpl(
    private val mangaResourceDao: SeasonalMangaResourceDao,
    private val amadeusDatabase: AmadeusDatabase,
    private val seasonalListDao: SeasonalListDao,
    private val mangaDexApi: MangaDexApi,
    private val dispatchers: AmadeusDispatchers
): SeasonalMangaRepository {

    override fun observeMangaResourceById(id: String): Flow<SeasonalMangaResource?> {
        return mangaResourceDao.observeSeasonalMangaResourceById(id)
    }

    override fun observeAllMangaResources(): Flow<List<SeasonalMangaResource>> {
        return mangaResourceDao.getSeasonalMangaResources()
    }

    override fun getSeasonalLists(): Flow<List<SeasonListWithManga>> {
        return seasonalListDao.observeSeasonListWithManga()
    }

    private fun syncer(list: Data) = syncerForEntity<SeasonalMangaResource, Manga, String>(
        networkToKey = { it.id },
        mapper = { manga, _ ->
            manga.toSeasonalMangaResource().copy(seasonId = list.id)
        },
        upsert = { mangaResourceDao.upsertSeasonalMangaResource(it) }
    )

    private fun String.toSeason() = when {
        this.contains("winter", ignoreCase = true) -> Season.Winter
        this.contains("summer", ignoreCase = true) -> Season.Summer
        this.contains("fall", ignoreCase = true) -> Season.Fall
        this.contains("spring", ignoreCase = true) -> Season.Spring
        else -> null
    }

    private suspend fun updateSeasonalList(
        seasonalLists: List<Triple<Season, Int, Data>>
    ) {
        withContext(dispatchers.io) {
            // fetch all manga by id from the lists in chunks of 100 which is max per request
            val response = seasonalLists
                .map { (_, _, data) ->
                    data.relationships
                        .filter { it.type == "manga" }
                        .map { it.id }
                }
                .flatten()
                .chunked(100)
                .pmap {
                    mangaDexApi.getMangaList(
                        MangaRequest(
                            limit = 100,
                            ids = it,
                            includes = listOf("cover_art", "author", "artist"),
                            order = mapOf("followedCount" to "desc"),
                            hasAvailableChapters = true
                        )
                    )
                        .getOrThrow()
                        .data
                }
                .flatten()
            amadeusDatabase.withTransaction {

                seasonalListDao.clear()
                mangaResourceDao.clear()

                val seasonalBeforeUpdate = seasonalListDao.getSeasonListWithManga()

                seasonalLists.forEach { (season, year, list) ->
                    seasonalListDao.upsertSeasonalList(
                        SeasonalListEntity(
                            id = list.id,
                            year = year,
                            season = season,
                        )
                    )
                    syncer(list).sync(
                        current = seasonalBeforeUpdate
                            .find { seasonalList -> seasonalList.list.id == list.id }
                            ?.manga ?: emptyList(),
                        networkResponse = response.filter { manga ->
                            manga.id in list.relationships
                                .filter { relationship -> relationship.type == "manga" }
                                .map { relationship -> relationship.id }
                        },
                    )
                }
            }
        }
    }

    override suspend fun sync(): Boolean {
        return suspendRunCatching {
            val mangaDexLists = mangaDexApi
                .getUserLists("d2ae45e0-b5e2-4e7f-a688-17925c2d7d6b")
                .getOrThrow()
                .data
                .mapNotNull { userList ->
                    Triple(
                       /*Season*/ userList.attributes.name.toSeason() ?: return@mapNotNull null,
                        /*year*/  userList.attributes.name.filter { c -> c.isDigit() }.toIntOrNull() ?: return@mapNotNull null,
                        /*list*/  userList
                    )
                }
            val seasonalLists = mangaDexLists.groupBy { (_, year, _) -> year }
                .mapValues { (_, triple) ->
                    triple.sortedByDescending { (season, _, _) ->  season.ordinal }
                }
                .flatMap { (_, triple) -> triple }
                .take(4)

            updateSeasonalList(seasonalLists)
        }
            .isSuccess
    }

}