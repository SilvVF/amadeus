package io.silv.manga.domain.repositorys

import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.MangaToSeasonalMangaResourceMapper
import io.silv.manga.local.dao.SeasonalMangaResourceDao
import io.silv.manga.local.entity.SeasonalMangaResource
import io.silv.manga.local.entity.syncerForEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.network.mangadex.requests.MangaRequest
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal class SeasonalMangaRepositoryImpl(
    private val mangaResourceDao: SeasonalMangaResourceDao,
    private val mangaDexApi: MangaDexApi,
    dispatchers: AmadeusDispatchers
): SeasonalMangaRepository {

    private val syncer = syncerForEntity<SeasonalMangaResource, Manga, String>(
        networkToKey = { it.id },
        mapper = { manga, seasonalMangaResource ->
            MangaToSeasonalMangaResourceMapper.map(manga to seasonalMangaResource)
        },
        upsert = {
            mangaResourceDao.upsertManga(it)
        }
    )
    private val scope = CoroutineScope(dispatchers.io) + CoroutineName("SeasonalMangaRepositoryImpl")

    override fun getMangaResource(id: String): Flow<SeasonalMangaResource?> {
        return mangaResourceDao.getResourceAsFlowById(id)
    }

    override fun getMangaResources(): Flow<List<SeasonalMangaResource>> {
        return mangaResourceDao.getMangaResources()
    }

    private val mutableLoading = MutableStateFlow(false)
    override val loading: Flow<Boolean> = mutableLoading.asStateFlow()

    private fun seasonFromMonth(month: Int): String = when(month) {
        in listOf(12, 1 ,2) -> "fall"
        in listOf(3, 4, 5) -> "winter"
        in listOf(6, 7 ,8) -> "spring"
        else -> "summer"
    }

    override suspend fun refreshList() {
        loadSeasonalManga()
    }

    private suspend fun loadSeasonalManga() = withContext(scope.coroutineContext) {
        mutableLoading.emit(true)
        runCatching {
            val result = syncer.sync(
                current = mangaResourceDao.getAll(),
                networkResponse = run {
                    val mangaDexLists = mangaDexApi
                        .getUserLists("d2ae45e0-b5e2-4e7f-a688-17925c2d7d6b")
                        .getOrThrow()
                    println("Seasonal $mangaDexLists")

                    val timeNow = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val year = timeNow.year
                    val season = seasonFromMonth(timeNow.monthNumber)

                    println("Seasonal $season  $year")

                    val seasonalListInfo = mangaDexLists.data.find { data ->
                        data.attributes.name.contains(year.toString(), ignoreCase = true) &&
                        data.attributes.name.contains(season, ignoreCase = true)
                    } ?: error("couldn't find seasonal list")

                    println("Seasonal $seasonalListInfo")

                    val seasonalList = mangaDexApi.getListById(seasonalListInfo.id).getOrThrow()

                    println("Seasonal $seasonalList")


                    mangaDexApi.getMangaList(
                        MangaRequest(
                            limit = 100,
                            ids = seasonalList.data.relationships
                                .filter { it.type == "manga" }
                                .map { it.id },
                            includes = listOf("cover_art"),
                            order = mapOf("followedCount" to "desc"),
                            hasAvailableChapters = true
                        )
                    )
                        .getOrThrow()
                        .data.also {
                            println("Seasonal $it")
                        }
                }
            )
            for(unhandled in result.unhandled) {
                mangaResourceDao.delete(unhandled)
            }
        }
            .onFailure {
                it.printStackTrace()
            }
            .isSuccess
            .also { mutableLoading.emit(false) }
    }
}