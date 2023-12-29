package io.silv.data.manga

import android.util.Log
import androidx.room.withTransaction
import com.skydoves.sandwich.getOrThrow
import io.silv.common.AmadeusDispatchers
import io.silv.common.coroutine.suspendRunCatching
import io.silv.common.model.Season
import io.silv.data.mappers.toEntity
import io.silv.database.AmadeusDatabase
import io.silv.database.entity.list.SeasonalListEntity
import io.silv.database.entity.manga.remotekeys.SeasonalRemoteKey
import io.silv.domain.manga.repository.SeasonalMangaRepository
import io.silv.model.DomainSeasonalList
import io.silv.network.MangaDexApi
import io.silv.network.model.list.Data
import io.silv.network.util.fetchMangaChunked
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


internal class SeasonalMangaRepositoryImpl(
    private val mangaDexApi: MangaDexApi,
    private val db: AmadeusDatabase,
    private val dispatchers: AmadeusDispatchers,
): SeasonalMangaRepository {

    private val seasonalListDao = db.seasonalListDao()
    private val sourceMangaDao = db.sourceMangaDao()
    private val keyDao = db.seasonalRemoteKeysDao()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            subscribe().collect {
                Log.d("Seasonal", it.toString())
            }
        }
    }

    override fun subscribe() =
        seasonalListDao.observeSeasonListWithManga().map { lists ->
            lists.map { item ->
                DomainSeasonalList(
                    item.list.id,
                    item.list.season,
                    item.list.year,
                    item.manga.map { it.manga.let(MangaMapper::mapManga) }.toImmutableList()
                )
            }
        }

    private suspend fun updateSeasonalList(
        seasonalLists: List<SeasonInfo>
    ) {
        withContext(dispatchers.io) {

            val toUpdate = seasonalLists.filter {
                needsUpdate(seasonalListDao.getSeasonalLists(), it)
            }


            val response = mangaDexApi.fetchMangaChunked(
                ids = toUpdate.map { (_, _, data) ->
                    data.relationships
                        .filter { it.type == "manga" }
                        .map { it.id }
                }
                    .flatten(),
                chunkSize = 100
            )

            db.withTransaction {

                seasonalListDao.clearNonMathcingIds(
                    seasonalLists.map { it.userList.id }
                )

                seasonalLists.forEach { (season, year, userList) ->
                    seasonalListDao.upsertSeasonalList(
                        SeasonalListEntity(
                            id = userList.id,
                            year = year,
                            season = season,
                            version = userList.attributes.version
                        )
                    )
                }

                sourceMangaDao.insertAll(response.map { it.toEntity() })

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

    override suspend fun sync(): Boolean {
        return suspendRunCatching {

            val seasonInfoList = mangaDexApi
                .getUserLists(MANGA_DEX_ADMIN_USER).getOrThrow().data
                .toListSeasonInfo()


            val mostRecent = seasonInfoList.sortByMostRecent().take(4)

            updateSeasonalList(mostRecent)
        }
            .onFailure { it.printStackTrace() }
            .isSuccess
    }

    private fun List<SeasonInfo>.sortByMostRecent() =
        groupBy {  it.year }
            .mapValues { (_, infoList) ->
                infoList.sortedByDescending { it.season.ordinal }
            }
            .flatMap { (_, infoList) -> infoList }

    private fun List<Data>.toListSeasonInfo() = mapNotNull { userList ->
        SeasonInfo(
            season = userList.attributes.name.toSeason() ?: return@mapNotNull null,
            year =  userList.attributes.name.filter { c -> c.isDigit() }.toIntOrNull() ?: return@mapNotNull null,
            userList = userList
        )
    }

    private fun needsUpdate(prevLists: List<SeasonalListEntity>, info: SeasonInfo): Boolean {
        val prev = prevLists.find { it.id == info.userList.id }
        return prev == null || prev.version != info.userList.attributes.version
    }

    companion object {

        private data class SeasonInfo(
            val season: Season,
            val year: Int,
            val userList: Data
        )

        private fun String.toSeason() = when {
            this.contains("winter", ignoreCase = true) -> Season.Winter
            this.contains("summer", ignoreCase = true) -> Season.Summer
            this.contains("fall", ignoreCase = true) -> Season.Fall
            this.contains("spring", ignoreCase = true) -> Season.Spring
            else -> null
        }

        const val MANGA_DEX_ADMIN_USER = "d2ae45e0-b5e2-4e7f-a688-17925c2d7d6b"
    }
}