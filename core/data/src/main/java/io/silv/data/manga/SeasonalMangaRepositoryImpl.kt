package io.silv.data.manga

import androidx.room.withTransaction
import io.silv.common.coroutine.suspendRunCatching
import io.silv.common.model.Season
import io.silv.common.pmap
import io.silv.data.mappers.toSourceManga
import io.silv.data.util.createSyncer
import io.silv.database.AmadeusDatabase
import io.silv.database.dao.SeasonalListDao
import io.silv.database.dao.SourceMangaDao
import io.silv.database.dao.remotekeys.SeasonalRemoteKeysDao
import io.silv.database.entity.list.SeasonalListEntity
import io.silv.database.entity.manga.SourceMangaResource
import io.silv.database.entity.manga.remotekeys.SeasonalRemoteKey
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.network.model.list.Data
import io.silv.network.model.manga.Manga
import io.silv.network.requests.MangaRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext


internal class SeasonalMangaRepositoryImpl(
    private val mangaResourceDao: SourceMangaDao,
    private val amadeusDatabase: AmadeusDatabase,
    private val seasonalListDao: SeasonalListDao,
    private val seasonalRemoteKeysDao: SeasonalRemoteKeysDao,
    private val mangaDexApi: io.silv.network.MangaDexApi,
    private val dispatchers: io.silv.common.AmadeusDispatchers
): SeasonalMangaRepository {

    override fun getSeasonalLists(): Flow<List<Pair<SeasonalListEntity, List<SourceMangaResource>>>> {
        return seasonalListDao.getSeasonalLists().map { lists ->
            lists.map { list ->
                list to seasonalRemoteKeysDao.selectBySeasonId(list.id)
            }
        }
    }

    private fun syncer(list: Data) = createSyncer<SourceMangaResource, Manga, String>(
            networkToKey = { it.id },
            mapper = { manga, _ ->
                manga.toSourceManga()
            },
            upsert = {
                mangaResourceDao.insert(it)
                seasonalRemoteKeysDao.insert(
                    SeasonalRemoteKey(
                        mangaId = it.id,
                        seasonId = list.id
                    )
                )
            }
        )

    private fun String.toSeason() = when {
        this.contains("winter", ignoreCase = true) -> Season.Winter
        this.contains("summer", ignoreCase = true) -> Season.Summer
        this.contains("fall", ignoreCase = true) -> Season.Fall
        this.contains("spring", ignoreCase = true) -> Season.Spring
        else -> null
    }

    private suspend fun fetchChuncked(seasonalLists: List<Triple<Season, Int, Data>>): List<Manga> {
       return seasonalLists
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
    }

    private suspend fun updateSeasonalList(
        seasonalLists: List<Triple<Season, Int, Data>>
    ) {
        withContext(dispatchers.io) {

            val response = fetchChuncked(seasonalLists)

            amadeusDatabase.withTransaction {

                seasonalLists.forEach { (season, year, list) ->

                    seasonalListDao.upsertSeasonalList(
                        SeasonalListEntity(
                            id = list.id,
                            year = year,
                            season = season,
                        )
                    )
                    val result = syncer(list).sync(
                        current = seasonalRemoteKeysDao.selectBySeasonId(list.id),
                        networkResponse = response.filter { manga ->
                            manga.id in list.relationships
                                .filter { relationship -> relationship.type == "manga" }
                                .map { relationship -> relationship.id }
                        },
                    )

                    for (item in result.unhandled) {
                        seasonalRemoteKeysDao.delete(item.id, list.id)
                    }
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