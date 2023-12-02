package io.silv.data.manga

import androidx.room.withTransaction
import io.silv.common.AmadeusDispatchers
import io.silv.common.coroutine.suspendRunCatching
import io.silv.common.model.Season
import io.silv.common.pmap
import io.silv.data.mappers.toSourceManga
import io.silv.database.AmadeusDatabase
import io.silv.database.entity.list.SeasonalListEntity
import io.silv.database.entity.manga.SourceMangaResource
import io.silv.database.entity.manga.remotekeys.SeasonalRemoteKey
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.network.MangaDexApi
import io.silv.network.model.list.Data
import io.silv.network.model.manga.Manga
import io.silv.network.requests.MangaRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext


internal class SeasonalMangaRepositoryImpl(
    private val mangaDexApi: MangaDexApi,
    private val db: AmadeusDatabase,
    private val dispatchers: AmadeusDispatchers,
): SeasonalMangaRepository {

    private val seasonalListDao = db.seasonalListDao()
    private val sourceMangaDao = db.sourceMangaDao()
    private val keyDao = db.seasonalRemoteKeysDao()

    override fun getSeasonalLists(): Flow<List<Pair<SeasonalListEntity, List<SourceMangaResource>>>> {
        return seasonalListDao.observeSeasonListWithManga().map { lists ->
            lists.map { listWithKeys ->
                listWithKeys.list to listWithKeys.manga.map { it.manga }
            }
        }
    }


    private fun String.toSeason() = when {
        this.contains("winter", ignoreCase = true) -> Season.Winter
        this.contains("summer", ignoreCase = true) -> Season.Summer
        this.contains("fall", ignoreCase = true) -> Season.Fall
        this.contains("spring", ignoreCase = true) -> Season.Spring
        else -> null
    }

    private suspend fun fetchChunked(seasonalLists: List<SeasonInfo>): List<Manga> {
       return seasonalLists
           .asSequence()
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
        seasonalLists: List<SeasonInfo>
    ) {
        withContext(dispatchers.io) {

            val response = fetchChunked(seasonalLists)

            seasonalListDao.clearNonMathcingIds(seasonalLists.map { it.userList.id })

            seasonalLists.forEach { (season, year, userList) ->
                seasonalListDao.upsertSeasonalList(
                    SeasonalListEntity(
                        id = userList.id,
                        year = year,
                        season = season,)
                )
            }

            db.withTransaction {
                sourceMangaDao.insertAll(response.map { it.toSourceManga() })

                keyDao.insertAll(
                    response.map {
                        SeasonalRemoteKey(
                            mangaId = it.id,
                            seasonId = seasonalLists.find { list ->
                                list.userList.relationships
                                    .any { r -> r.type == "manga" && r.id == it.id }

                            }?.userList?.id!!,
                        )
                    }
                )
            }
        }
    }

    private data class SeasonInfo(
        val season: Season,
        val year: Int,
        val userList: Data
    )

    override suspend fun sync(): Boolean {
        return suspendRunCatching {

            val seasonInfoList = mangaDexApi
                .getUserLists(MANGA_DEX_ADMIN_USER)
                .getOrThrow()
                .data
                .mapNotNull { userList ->
                    SeasonInfo(
                        season = userList.attributes.name.toSeason() ?: return@mapNotNull null,
                        year =  userList.attributes.name.filter { c -> c.isDigit() }.toIntOrNull() ?: return@mapNotNull null,
                        userList = userList
                    )
                }

            val mostRecentLists = seasonInfoList.groupBy {  it.year }
                .mapValues { (_, infoList) ->
                    infoList.sortedByDescending { it.season.ordinal }
                }
                .flatMap { (_, infoList) -> infoList }
                .take(4)

            updateSeasonalList(mostRecentLists)
        }
            .onFailure { it.printStackTrace() }
            .isSuccess
    }
    companion object{
        const val MANGA_DEX_ADMIN_USER = "d2ae45e0-b5e2-4e7f-a688-17925c2d7d6b"
    }
}