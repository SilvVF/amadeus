package io.silv.manga.domain.repositorys

import io.silv.core.AmadeusDispatchers
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.domain.MangaToRecentMangaResourceMapper
import io.silv.manga.domain.checkProtected
import io.silv.manga.domain.repositorys.base.BasePaginatedRepository
import io.silv.manga.local.dao.RecentMangaResourceDao
import io.silv.manga.local.entity.RecentMangaResource
import io.silv.manga.local.entity.syncerForEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.network.mangadex.requests.MangaRequest
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

internal class RecentMangaRepositoryImpl(
    private val mangaDexApi: MangaDexApi,
    private val mangaResourceDao: RecentMangaResourceDao,
    dispatchers: AmadeusDispatchers,
): RecentMangaRepository, BasePaginatedRepository<RecentMangaResource, Any?>(
    initialQuery = null
) {

    override val scope: CoroutineScope = CoroutineScope(dispatchers.io) + CoroutineName("RecentMangaRepositoryImpl")

    override fun observeMangaResources(resourceQuery: Any?): Flow<List<RecentMangaResource>> {
        return mangaResourceDao.getRecentMangaResources()
    }

    private val mapper = MangaToRecentMangaResourceMapper

    private val syncer = syncerForEntity<RecentMangaResource, Manga, String>(
        networkToKey = { n -> n.id },
        mapper = { manga, resource -> mapper.map(manga to resource) },
        upsert = { mangaResourceDao.upsertRecentMangaResource(it) }
    )

    init {
        scope.launch {
            refresh()
        }
    }

    override fun observeMangaResourceById(id: String): Flow<RecentMangaResource?> {
        return mangaResourceDao.getRecentMangaResourceById(id)
    }

    override fun observeAllMangaResources(): Flow<List<RecentMangaResource>> {
        return mangaResourceDao.getRecentMangaResources()
    }

    override suspend fun refresh() {
        resetPagination(null)
        loadNextPage()
    }

    override suspend fun loadNextPage() = loadPage { offset, _ ->
            val result = syncer.sync(
                current = mangaResourceDao.getRecentMangaResources().first(),
                networkResponse = mangaDexApi.getMangaList(
                    MangaRequest(
                        offset = offset,
                        limit = MANGA_PAGE_LIMIT,
                        includes = listOf("cover_art","author", "artist"),
                        availableTranslatedLanguage = listOf("en"),
                        order = mapOf("createdAt" to "desc")
                    )
                )
                    .getOrThrow()
                    .also {
                        updateLastPage(it.total)
                    }
                    .data
            )
            if (offset == 0) {
                for (unhandled in result.unhandled) {
                    if (!checkProtected(unhandled.id)) {
                        mangaResourceDao.deleteRecentMangaResource(unhandled)
                    }
                }
            }
    }
}