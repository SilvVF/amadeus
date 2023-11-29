package io.silv.data.manga

import io.silv.common.model.LoadState
import io.silv.common.time.localDateTimeNow
import io.silv.common.time.minus
import io.silv.common.time.timeStringMinus
import io.silv.data.mappers.toFilteredMangaYearlyResource
import io.silv.data.util.createSyncer
import io.silv.database.entity.manga.resource.FilteredMangaYearlyResource
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.network.model.manga.Manga
import io.silv.network.requests.MangaRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days

internal class FilteredYearlyMangaRepositoryImpl(
    private val yearlyResourceDao: io.silv.database.dao.FilteredMangaYearlyResourceDao,
    private val mangaDexApi: io.silv.network.MangaDexApi,
    dispatchers: io.silv.common.AmadeusDispatchers,
): FilteredYearlyMangaRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    override val loadState = MutableStateFlow<LoadState>(LoadState.None)

    private fun syncerForYearly(tag: String) = createSyncer<FilteredMangaYearlyResource, Pair<Manga, Int>, String>(
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
        val now = localDateTimeNow()
        val lastUpdate = yearly.maxByOrNull { it.updatedAt.date.toEpochDays() }?.updatedAt

        if (lastUpdate != null && now - lastUpdate < 1.days) {
            return
        }

        loadState.emit(LoadState.Loading)

        val result = syncerForYearly(tagId).sync(
            current = yearly,
            networkResponse = mangaDexApi.getMangaList(
                MangaRequest(
                    offset = 0,
                    limit = 20,
                    includes = listOf("cover_art", "author", "artist"),
                    availableTranslatedLanguage = listOf("en"),
                    hasAvailableChapters = true,
                    order = mapOf("followedCount" to "desc"),
                    includedTags = listOf(tagId),
                    includedTagsMode = MangaRequest.TagsMode.AND,
                    createdAtSince = timeStringMinus(365.days)
                )
            )
                .getOrThrow()
                .data
                .mapIndexed { i, manga ->
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
        loadState.emit(LoadState.None)
    }

    override fun observeMangaResourceById(id: String): Flow<FilteredMangaYearlyResource?> {
        return yearlyResourceDao.observeFilteredYearlyMangaResourceById(id)
    }

    override fun observeAllMangaResources(): Flow<List<FilteredMangaYearlyResource>> {
        return yearlyResourceDao.getFilteredMangaYearlyResources()
    }

    override suspend fun refresh() = Unit
}