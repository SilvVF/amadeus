package io.silv.manga.repositorys.manga

import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.local.dao.FilteredMangaYearlyResourceDao
import io.silv.manga.local.entity.manga_resource.FilteredMangaYearlyResource
import io.silv.manga.local.entity.syncerForEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.network.mangadex.requests.MangaRequest
import io.silv.manga.repositorys.timeNow
import io.silv.manga.repositorys.timeStringMinus
import io.silv.manga.repositorys.toFilteredMangaYearlyResource
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.time.Duration
import kotlin.time.toKotlinDuration

internal class FilteredYearlyMangaRepositoryImpl(
    private val yearlyResourceDao: FilteredMangaYearlyResourceDao,
    private val mangaDexApi: MangaDexApi,
    dispatchers: AmadeusDispatchers,
): FilteredYearlyMangaRepository {

    private val scope: CoroutineScope =
        CoroutineScope(dispatchers.io) + CoroutineName("FilteredYearlyMangaRepositoryImpl")

    override val loadState = MutableStateFlow<io.silv.manga.repositorys.LoadState>(io.silv.manga.repositorys.LoadState.None)

    private fun syncerForYearly(tag: String) = syncerForEntity<FilteredMangaYearlyResource, Pair<Manga, Int>, String>(
        networkToKey = { (manga, _) -> manga.id },
        mapper = { (manga, placement), prev ->
            val resource = manga.toFilteredMangaYearlyResource(prev)
            resource.copy(
                topTagPlacement = resource.topTagPlacement
                    .toMutableMap()
                    .apply { put(tag, placement) },
                topTags = (resource.topTags + tag).toSet().toList()
            )
        },
        upsert = {
            yearlyResourceDao.upsertFilteredYearlyMangaResource(it)
        }
    )

    override fun getYearlyTopResources(tag: String): Flow<List<FilteredMangaYearlyResource>> {
        return yearlyResourceDao.getFilteredMangaYearlyResources()
            .map { it.filter { m ->  m.topTags.contains(tag) }.sortedBy { it.topTagPlacement[tag] } }
            .onStart {
                scope.launch {
                    loadYearlyTopManga(tag)
                }
            }
    }

    private suspend fun loadYearlyTopManga(tagId: String) {
        val yearly = yearlyResourceDao.getFilteredMangaYearlyResources().first()
        val instant = timeNow().toInstant(TimeZone.currentSystemDefault())
        val lastUpdated = (yearly
            .filter { it.topTags.contains(tagId) }
            .takeIf { it.isNotEmpty() }
            ?.minByOrNull { it.savedAtLocal }?.savedAtLocal ?: timeNow()
        )
            .toInstant(TimeZone.currentSystemDefault())
        if (instant.minus(lastUpdated) > Duration.ofDays(1).toKotlinDuration()) {
            return
        }
        loadState.emit(io.silv.manga.repositorys.LoadState.Loading)
        runCatching {
            val result = syncerForYearly(tagId).sync(
                current = yearly,
                networkResponse = mangaDexApi.getMangaList(
                    MangaRequest(
                        offset = 0,
                        limit = 20,
                        includes = listOf("cover_art","author", "artist"),
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
                    if (unhandled.topTags.size <= 1) {
                        yearlyResourceDao.deleteFilteredYearlyMangaResource(unhandled)
                    } else {
                        yearlyResourceDao.updateFilteredYearlyMangaResource(
                            unhandled.copy(
                                topTags = unhandled.topTags.filter { it != tagId }
                            )
                        )
                    }
                }
            }
        }
            .isSuccess
            .also { loadState.emit(io.silv.manga.repositorys.LoadState.None) }
    }

    override fun observeMangaResourceById(id: String): Flow<FilteredMangaYearlyResource?> {
        return yearlyResourceDao.observeFilteredYearlyMangaResourceById(id)
    }

    override fun observeAllMangaResources(): Flow<List<FilteredMangaYearlyResource>> {
        return yearlyResourceDao.getFilteredMangaYearlyResources()
    }

    override suspend fun refresh() = Unit
}