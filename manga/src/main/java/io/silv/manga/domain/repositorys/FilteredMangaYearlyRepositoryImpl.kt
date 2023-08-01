package io.silv.manga.domain.repositorys

import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.MangaToFilteredYearlyMangaResourceMapper
import io.silv.manga.domain.checkProtected
import io.silv.manga.domain.repositorys.base.LoadState
import io.silv.manga.domain.timeStringMinus
import io.silv.manga.local.dao.FilteredMangaYearlyResourceDao
import io.silv.manga.local.entity.FilteredMangaYearlyResource
import io.silv.manga.local.entity.syncerForEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.network.mangadex.requests.MangaRequest
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.datetime.Clock
import java.time.Duration

internal class FilteredYearlyMangaRepositoryImpl(
    private val yearlyResourceDao: FilteredMangaYearlyResourceDao,
    private val mangaDexApi: MangaDexApi,
    dispatchers: AmadeusDispatchers,
): FilteredYearlyMangaRepository {

    private val scope: CoroutineScope =
        CoroutineScope(dispatchers.io) + CoroutineName("FilteredYearlyMangaRepositoryImpl")

    override val loadState = MutableStateFlow<LoadState>(LoadState.None)

    private fun syncerForYearly(tag: String) = syncerForEntity<FilteredMangaYearlyResource, Pair<Manga, Int>, String>(
        networkToKey = { (manga, _) -> manga.id },
        mapper = { (manga, placement), saved ->
            val r = MangaToFilteredYearlyMangaResourceMapper.map(manga to saved)
            r.copy(
                topTagPlacement = r.topTagPlacement.toMutableMap().apply {
                    put(tag, placement)
                },
                topTags = (r.topTags + tag).toSet().toList()
            )
        },
        upsert = {
            yearlyResourceDao.upsertManga(it)
        }
    )

    override fun getYearlyTopResources(tag: String): Flow<List<FilteredMangaYearlyResource>> {
        return yearlyResourceDao.getMangaResources()
            .map { it.filter { m ->  m.topTags.contains(tag) }.sortedBy { it.topTagPlacement[tag] } }
            .onStart {
                scope.launch {
                    loadYearlyTopManga(tag)
                }
            }
    }

    private suspend fun loadYearlyTopManga(tagId: String) {
        val lastUpdated = yearlyResourceDao.getAll()
            .filter { it.topTags.contains(tagId) }
            .takeIf { it.isNotEmpty() }
            ?.minBy { it.savedLocalAtEpochSeconds }?.savedLocalAtEpochSeconds ?: 0L
        if (Clock.System.now().epochSeconds - lastUpdated < 60 * 60 * 24 * 30) {
            return
        }
        loadState.emit(LoadState.Loading)
        runCatching {
            val result = syncerForYearly(tagId).sync(
                current = yearlyResourceDao.getAll(),
                networkResponse = mangaDexApi.getMangaList(
                    MangaRequest(
                        offset = 0,
                        limit = 20,
                        includes = listOf("cover_art"),
                        availableTranslatedLanguage = listOf("en"),
                        hasAvailableChapters = true,
                        order = mapOf("followedCount" to "desc"),
                        includedTags = listOf(tagId),
                        includedTagsMode = MangaRequest.TagsMode.AND,
                        createdAtSince = timeStringMinus(Duration.ofDays(365))
                    )
                )
                    .getOrThrow()
                    .data.mapIndexed { i, manga->
                        manga to i + 1
                    }
            )
            for(unhandled in result.unhandled) {
                if (unhandled.topTags.contains(tagId)) {
                    if (unhandled.topTags.size <= 1 && !checkProtected(unhandled.id)) {
                        yearlyResourceDao.delete(unhandled)
                    } else {
                        yearlyResourceDao.update(
                            unhandled.copy(
                                topTags = unhandled.topTags.filter { it != tagId }
                            )
                        )
                    }
                }
            }
        }
            .isSuccess
            .also { loadState.emit(LoadState.None) }
    }

    override fun getMangaResource(id: String): Flow<FilteredMangaYearlyResource?> {
        return yearlyResourceDao.getResourceAsFlowById(id)
    }

    override fun getAllMangaResources(): Flow<List<FilteredMangaYearlyResource>> {
        return yearlyResourceDao.getMangaResources()
    }

    override suspend fun refresh() = Unit
}