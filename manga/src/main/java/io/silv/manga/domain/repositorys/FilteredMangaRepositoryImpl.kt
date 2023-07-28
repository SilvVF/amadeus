package io.silv.manga.domain.repositorys

import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.MangaToFilteredMangaResourceMapper
import io.silv.manga.local.dao.FilteredMangaResourceDao
import io.silv.manga.local.entity.FilteredMangaResource
import io.silv.manga.local.entity.syncerForEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.network.mangadex.requests.MangaRequest
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext

internal class FilteredMangaRepositoryImpl(
    private val resourceDao: FilteredMangaResourceDao,
    private val mangaDexApi: MangaDexApi,
    dispatchers: AmadeusDispatchers
): FilteredMangaRepository {

    private val scope = CoroutineScope(dispatchers.io) + CoroutineName("FilteredMangaRepositoryImpl")

    private val syncer = syncerForEntity<FilteredMangaResource, Manga, String>(
        networkToKey = { it.id },
        mapper = { manga, saved ->
            MangaToFilteredMangaResourceMapper.map(manga to saved)
        },
        upsert = {
            resourceDao.upsertManga(it)
        }
    )

    private var currentOffset = 0
    private val MANGA_PAGE_LIMIT = 50
    private var currentTag = ""

    override fun getMangaResource(id: String): Flow<FilteredMangaResource?> {
        return resourceDao.getResourceAsFlowById(id)
    }

    override fun getMangaResources(
        tag: String
    ): Flow<List<FilteredMangaResource>> {
        currentOffset = 0
        currentTag = tag
        scope.launch { loadNextPage() }
        return resourceDao.getMangaResources().map { it.filter { m -> m.tagToId.containsValue(tag) } }
    }

    override suspend fun loadNextPage(): Boolean = withContext(scope.coroutineContext) {
        if (currentTag.isBlank()) {
            return@withContext false
        }
        runCatching {
            val result = syncer.sync(
                current = resourceDao.getAll(),
                networkResponse = mangaDexApi.getMangaList(
                    MangaRequest(
                        offset = currentOffset,
                        limit = MANGA_PAGE_LIMIT,
                        includes = listOf("cover_art"),
                        availableTranslatedLanguage = listOf("en"),
                        hasAvailableChapters = true,
                        order = mapOf("followedCount" to "desc"),
                        includedTags = listOf(currentTag),
                        includedTagsMode = MangaRequest.TagsMode.AND
                    )
                )
                    .getOrThrow()
                    .data
                    .also {
                        println("Filtered" + it.size)
                    }
            )

            // Initial load delete previous resources
            // if at this point network response was successful
            if (currentOffset == 0) {
                for(unhandled in result.unhandled) {
                    if (!unhandled.tagToId.containsKey(currentTag)) {
                        resourceDao.delete(unhandled)
                    }
                }
            }
            // Increase offset after successful sync
            currentOffset += MANGA_PAGE_LIMIT
        }
            .isSuccess
    }
}