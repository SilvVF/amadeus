package io.silv.manga.domain.repositorys

import android.util.Log
import io.silv.core.AmadeusDispatchers
import io.silv.core.pmap
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.MangaToSeasonalMangaResourceMapper
import io.silv.manga.domain.checkProtected
import io.silv.manga.domain.repositorys.base.LoadState
import io.silv.manga.domain.suspendRunCatching
import io.silv.manga.domain.timeNow
import io.silv.manga.local.dao.SeasonalListDao
import io.silv.manga.local.dao.SeasonalMangaResourceDao
import io.silv.manga.local.entity.Season
import io.silv.manga.local.entity.SeasonalListEntity
import io.silv.manga.local.entity.SeasonalMangaResource
import io.silv.manga.local.entity.relations.SeasonListWithManga
import io.silv.manga.local.entity.syncerForEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.list.Data
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.network.mangadex.requests.MangaRequest
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext


internal class SeasonalMangaRepositoryImpl(
    private val mangaResourceDao: SeasonalMangaResourceDao,
    private val seasonalListDao: SeasonalListDao,
    private val mangaDexApi: MangaDexApi,
    private val dispatchers: AmadeusDispatchers
): SeasonalMangaRepository {

    private val scope = CoroutineScope(dispatchers.io) + CoroutineName("SeasonalMangaRepositoryImpl")

    override val loadState = MutableStateFlow<LoadState>(LoadState.None)

    fun syncer(list: Data) = syncerForEntity<SeasonalMangaResource, Manga, String>(
        networkToKey = { it.id },
        mapper = { manga, saved ->
            MangaToSeasonalMangaResourceMapper.map(manga to saved).copy(
                seasonId = list.id
            )
        },
        upsert = {
            mangaResourceDao.upsertSeasonalMangaResource(it)
        }
    )

    override suspend fun refresh() {
        getLatestLists()
    }

    private fun String.toSeason() = when {
        this.contains("winter", ignoreCase = true) -> Season.Winter
        this.contains("summer", ignoreCase = true) -> Season.Summer
        this.contains("fall", ignoreCase = true) -> Season.Fall
        this.contains("spring", ignoreCase = true) -> Season.Spring
        else -> null
    }

    private suspend fun updateSeason(request: List<Pair<Season, Int>>) = suspendRunCatching {
        val seasonYearToList = mutableMapOf<Pair<Season, Int>, Data>()
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
            s to y in request
        }.forEach { (s, y, list) ->
            seasonYearToList[s to y] = list
        }
        val response = seasonYearToList.values.toList()
            .map { data ->
                data.relationships
                    .filter { it.type == "manga" }
                    .map { it.id }
            }
            .flatten()
            .chunked(100).pmap {
                mangaDexApi.getMangaList(
                    MangaRequest(
                        limit = 100,
                        ids = it,
                        includes = listOf("cover_art","author", "artist"),
                        order = mapOf("followedCount" to "desc"),
                        hasAvailableChapters = true
                    )
                )
                    .getOrThrow()
                    .data
            }
             .flatten()

        Log.d("SEASONS", seasonYearToList.toString())
        seasonYearToList.forEach { (seasonToYear, list) ->
            seasonalListDao.upsertSeasonalList(
                SeasonalListEntity(
                    id = list.id,
                    year = seasonToYear.second,
                    season = seasonToYear.first,
                )
            )
            val result = syncer(list).sync(
                current = seasonalListDao.getSeasonListsWithManga().first().find { it.list.id == list.id }?.manga ?: emptyList(),
                networkResponse = response.filter { it.id in list.relationships.filter { it.type == "manga" }.map { it.id } },
            )
            for (unhandled in result.unhandled) {
                if (!checkProtected(unhandled.id)) {
                    mangaResourceDao.deleteSeasonalMangaResource(unhandled)
                }
            }
        }
    }
        .onFailure { it.printStackTrace() }

    override suspend fun updateSeasonList(season: Season, year: Int): Unit = withContext(dispatchers.io) {
        loadState.update { LoadState.Loading }
        if (seasonalListDao.getSeasonalLists().first().any { it.year == year  && it.season == season}) {
            return@withContext
        }
        updateSeason(listOf(season to year))
        loadState.update { LoadState.None }
    }

    private fun getLatestLists() = scope.launch {
        loadState.update { LoadState.Refreshing }
        val year = timeNow().year
        val toRequest = Season.values().map { it to year } + Season.values().map { it to year - 1}
        updateSeason(
            request = toRequest
        )
        loadState.update { LoadState.None }
    }

    override fun getSeasonalLists(): Flow<List<SeasonListWithManga>> {
        return seasonalListDao.getSeasonListsWithManga().onStart {
            if (mangaResourceDao.getSeasonalMangaResources().firstOrNull().isNullOrEmpty()) {
                refresh()
            }
        }
    }

    override fun observeMangaResourceById(id: String): Flow<SeasonalMangaResource?> {
        return mangaResourceDao.observeSeasonalMangaResourceById(id)
    }

    override fun observeAllMangaResources(): Flow<List<SeasonalMangaResource>> {
       return mangaResourceDao.getSeasonalMangaResources()
    }
}